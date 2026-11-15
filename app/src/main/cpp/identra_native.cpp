// Identra native property layer (0.7.0) — see docs/90-native-props.md on branch ops.
//
// Loaded by the module's Java code (System.loadLibrary) in a hooked app process. The framework
// (Vector / LSPosed native API) sees the library listed in META-INF/xposed/native_init.list and
// calls native_init() with an inline-hook function. We hook bionic's property readers and the
// popen() shell-out of "getprop", and answer them from a rule table the Java side pushes through
// JNI (the same PropRules the Java SystemProperties / getprop hooks use).
//
// Rules: exact key -> value, prefix -> value (longest prefix wins, prefixes before exact keys,
// like PropRules.mappingOf). Value "" = hide the key (reads as missing). Never fabricate: a key
// that is empty / missing on the real device stays missing. Until a table arrives, or on any
// error, every hook passes through to the real function.

#include <jni.h>
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <pthread.h>
#include <algorithm>
#include <atomic>
#include <string>
#include <vector>
#include <unordered_map>
#include <mutex>
#include <android/log.h>
#include <sys/system_properties.h>
#include <sys/utsname.h>
#include <sys/syscall.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdarg.h>
#include <errno.h>
#include <ctype.h>
#include <regex>

#define TAG "Identra.Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGD(...) do { if (g_debug) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__); } while (0)

// ---- framework native API (Vector native/include/core/native_api.h, API version 2) -----------
typedef int (*HookFunType)(void *func, void *replace, void **backup);
typedef int (*UnhookFunType)(void *func);
typedef void (*NativeOnModuleLoaded)(const char *name, void *handle);
struct NativeAPIEntries {
    uint32_t version;
    HookFunType hookFunc;
    UnhookFunType unhookFunc;
};

static const NativeAPIEntries *g_api = nullptr;
static bool g_debug = false;

// ---- rule table (immutable snapshot, swapped atomically, never freed) ---------------------------
struct Rule { std::string key; std::string value; };
struct Table {
    std::unordered_map<std::string, std::string> exact;
    std::vector<Rule> prefixes;   // longest first
    // 0.7.4: whole-file answers (path -> content) and uname fields; empty = leave alone
    std::unordered_map<std::string, std::string> files;
    std::string un_release, un_version, un_nodename;
    // 0.7.7: GL strings (glGetString answers; strdup'ed, never freed so old pointers stay valid)
    const char *gl_vendor = nullptr, *gl_renderer = nullptr, *gl_version = nullptr;
};
static std::atomic<Table *> g_table{nullptr};

// counters for the status string
static std::atomic<uint32_t> c_get{0}, c_cb{0}, c_read{0}, c_find_hidden{0}, c_foreach_hidden{0}, c_popen{0}, c_file{0}, c_uname{0}, c_gl{0};

// Returns nullptr when the key has no rule; otherwise the value ("" = hide).
static const std::string *rule_for(const char *name) {
    Table *t = g_table.load(std::memory_order_acquire);
    if (t == nullptr || name == nullptr || name[0] == 0) return nullptr;
    for (const Rule &p : t->prefixes)
        if (strncmp(name, p.key.c_str(), p.key.size()) == 0) return &p.value;
    auto it = t->exact.find(name);
    return it == t->exact.end() ? nullptr : &it->second;
}

// ---- originals ---------------------------------------------------------------------------------
typedef int (*get_t)(const char *, char *);
typedef const prop_info *(*find_t)(const char *);
typedef void (*cbfn_t)(void *, const char *, const char *, uint32_t);
typedef void (*read_cb_t)(const prop_info *, cbfn_t, void *);
typedef int (*read_t)(const prop_info *, char *, char *);
typedef int (*foreach_t)(void (*)(const prop_info *, void *), void *);
typedef FILE *(*popen_t)(const char *, const char *);
typedef int (*pclose_t)(FILE *);

static get_t o_get = nullptr;
static find_t o_find = nullptr;
static read_cb_t o_read_cb = nullptr;
static read_t o_read = nullptr;
static foreach_t o_foreach = nullptr;
static popen_t o_popen = nullptr;
static pclose_t o_pclose = nullptr;
typedef int (*uname_t)(struct utsname *);
typedef int (*open_t)(const char *, int, ...);
typedef int (*openat_t)(int, const char *, int, ...);
typedef int (*open2_t)(const char *, int);
typedef int (*openat2_t)(int, const char *, int);
typedef FILE *(*fopen_t)(const char *, const char *);
static uname_t o_uname = nullptr;
static open_t o_open = nullptr;
static openat_t o_openat = nullptr;
static open2_t o_open2 = nullptr;
static openat2_t o_openat2 = nullptr;
static fopen_t o_fopen = nullptr;
static void *s_uname = nullptr, *s_open = nullptr, *s_openat = nullptr, *s_open2 = nullptr, *s_openat2 = nullptr, *s_fopen = nullptr;

static void *s_get = nullptr, *s_find = nullptr, *s_read_cb = nullptr, *s_read = nullptr,
            *s_foreach = nullptr, *s_popen = nullptr, *s_pclose = nullptr;
static std::atomic<bool> g_hooked{false};
static std::string g_hook_errors;

// name of a prop_info through the real read_callback
struct NameBuf { char name[PROP_NAME_MAX * 4]; };
static void name_cb(void *cookie, const char *name, const char *, uint32_t) {
    NameBuf *b = (NameBuf *) cookie;
    if (name) { strncpy(b->name, name, sizeof(b->name) - 1); b->name[sizeof(b->name) - 1] = 0; }
}
static bool name_of(const prop_info *pi, NameBuf *b) {
    b->name[0] = 0;
    if (!pi || !o_read_cb) return false;
    o_read_cb(pi, name_cb, b);
    return b->name[0] != 0;
}

// ---- hooks -------------------------------------------------------------------------------------
static int h_get(const char *name, char *value) {
    int n = o_get(name, value);
    if (n <= 0 || value == nullptr) return n;
    const std::string *r = rule_for(name);
    if (r == nullptr) return n;
    c_get++;
    if (r->empty()) { value[0] = 0; return 0; }
    if (strcmp(value, r->c_str()) == 0) return n;
    size_t len = r->size();
    if (len > PROP_VALUE_MAX - 1) len = PROP_VALUE_MAX - 1;
    memcpy(value, r->data(), len);
    value[len] = 0;
    LOGD("get %s -> %s", name, value);
    return (int) len;
}

static const prop_info *h_find(const char *name) {
    const prop_info *pi = o_find(name);
    if (pi == nullptr) return pi;
    const std::string *r = rule_for(name);
    if (r != nullptr && r->empty()) { c_find_hidden++; LOGD("find %s -> hidden", name); return nullptr; }
    return pi;
}

struct CbCookie { cbfn_t cb; void *cookie; };
static void wrap_cb(void *c, const char *name, const char *value, uint32_t serial) {
    CbCookie *w = (CbCookie *) c;
    const std::string *r = (value && value[0]) ? rule_for(name) : nullptr;
    if (r != nullptr) {
        c_cb++;
        LOGD("read_callback %s -> %s", name, r->c_str());
        w->cb(w->cookie, name, r->c_str(), serial);
        return;
    }
    w->cb(w->cookie, name, value, serial);
}
static void h_read_cb(const prop_info *pi, cbfn_t cb, void *cookie) {
    if (cb == nullptr || g_table.load(std::memory_order_acquire) == nullptr) { o_read_cb(pi, cb, cookie); return; }
    CbCookie w{cb, cookie};
    o_read_cb(pi, wrap_cb, &w);
}

static int h_read(const prop_info *pi, char *name, char *value) {
    char localName[PROP_NAME_MAX];
    char *nm = name ? name : localName;
    int n = o_read(pi, nm, value);
    if (n <= 0 || value == nullptr) return n;
    const std::string *r = rule_for(nm);
    if (r == nullptr) return n;
    c_read++;
    if (r->empty()) { value[0] = 0; return 0; }
    size_t len = r->size();
    if (len > PROP_VALUE_MAX - 1) len = PROP_VALUE_MAX - 1;
    memcpy(value, r->data(), len);
    value[len] = 0;
    return (int) len;
}

struct ForeachCookie { void (*fn)(const prop_info *, void *); void *cookie; };
static void wrap_foreach(const prop_info *pi, void *c) {
    ForeachCookie *w = (ForeachCookie *) c;
    NameBuf b;
    if (name_of(pi, &b)) {
        const std::string *r = rule_for(b.name);
        if (r != nullptr && r->empty()) { c_foreach_hidden++; return; }
    }
    w->fn(pi, w->cookie);
}
static int h_foreach(void (*fn)(const prop_info *, void *), void *cookie) {
    if (fn == nullptr || g_table.load(std::memory_order_acquire) == nullptr) return o_foreach(fn, cookie);
    ForeachCookie w{fn, cookie};
    return o_foreach(wrap_foreach, &w);
}


// ---- 0.7.4: file answers + uname ---------------------------------------------------------------
static const std::string *file_for(const char *path) {
    Table *t = g_table.load(std::memory_order_acquire);
    if (t == nullptr || path == nullptr || t->files.empty()) return nullptr;
    auto it = t->files.find(path);
    return it == t->files.end() ? nullptr : &it->second;
}

// read-only fd with the content (memfd; the caller reads it like the real file)
static int content_fd(const std::string &content, int flags) {
    int fd = (int) syscall(__NR_memfd_create, "identra", (flags & O_CLOEXEC) ? 1U /*MFD_CLOEXEC*/ : 0U);
    if (fd < 0) return -1;
    size_t off = 0;
    while (off < content.size()) {
        ssize_t w = write(fd, content.data() + off, content.size() - off);
        if (w <= 0) { close(fd); return -1; }
        off += (size_t) w;
    }
    lseek(fd, 0, SEEK_SET);
    return fd;
}

static int try_serve(const char *path, int flags) {
    if ((flags & O_ACCMODE) != O_RDONLY) return -1;
    const std::string *c = file_for(path);
    if (c == nullptr) return -1;
    int fd = content_fd(*c, flags);
    if (fd >= 0) { c_file++; LOGD("open %s -> override (%zu bytes)", path, c->size()); }
    return fd;
}

static int h_open(const char *path, int flags, ...) {
    mode_t mode = 0;
    if ((flags & O_CREAT) || (flags & __O_TMPFILE) == __O_TMPFILE) { va_list ap; va_start(ap, flags); mode = (mode_t) va_arg(ap, int); va_end(ap); }
    int fd = try_serve(path, flags);
    if (fd >= 0) return fd;
    return o_open(path, flags, mode);
}
static int h_openat(int dirfd, const char *path, int flags, ...) {
    mode_t mode = 0;
    if ((flags & O_CREAT) || (flags & __O_TMPFILE) == __O_TMPFILE) { va_list ap; va_start(ap, flags); mode = (mode_t) va_arg(ap, int); va_end(ap); }
    if (path != nullptr && path[0] == '/') { int fd = try_serve(path, flags); if (fd >= 0) return fd; }
    return o_openat(dirfd, path, flags, mode);
}
static int h_open2(const char *path, int flags) {
    int fd = try_serve(path, flags);
    return fd >= 0 ? fd : o_open2(path, flags);
}
static int h_openat2(int dirfd, const char *path, int flags) {
    if (path != nullptr && path[0] == '/') { int fd = try_serve(path, flags); if (fd >= 0) return fd; }
    return o_openat2(dirfd, path, flags);
}
static FILE *h_fopen(const char *path, const char *mode) {
    if (mode != nullptr && mode[0] == 'r' && strchr(mode, '+') == nullptr) {
        int fd = try_serve(path, strchr(mode, 'e') ? O_RDONLY | O_CLOEXEC : O_RDONLY);
        if (fd >= 0) {
            FILE *f = fdopen(fd, "r");
            if (f != nullptr) return f;
            close(fd);
        }
    }
    return o_fopen(path, mode);
}

static void copy_field(char *dst, size_t cap, const std::string &v) {
    size_t n = std::min(v.size(), cap - 1);
    memcpy(dst, v.data(), n);
    dst[n] = 0;
}
static int h_uname(struct utsname *u) {
    int r = o_uname(u);
    if (r != 0 || u == nullptr) return r;
    Table *t = g_table.load(std::memory_order_acquire);
    if (t == nullptr || t->un_release.empty()) return r;
    copy_field(u->release, sizeof(u->release), t->un_release);
    copy_field(u->version, sizeof(u->version), t->un_version);
    if (!t->un_nodename.empty()) copy_field(u->nodename, sizeof(u->nodename), t->un_nodename);
    c_uname++;
    return r;
}

// ---- 0.7.7: glGetString (GLES 1/2/3 entry points) ------------------------------------------
typedef const unsigned char *(*glGetString_t)(unsigned int);
static glGetString_t o_gl1 = nullptr, o_gl2 = nullptr, o_gl3 = nullptr;
static void *s_gl1 = nullptr, *s_gl2 = nullptr, *s_gl3 = nullptr;
static const unsigned char *gl_answer(unsigned int name) {
    Table *t = g_table.load(std::memory_order_acquire);
    if (t == nullptr) return nullptr;
    const char *v = name == 0x1F00 ? t->gl_vendor : name == 0x1F01 ? t->gl_renderer : name == 0x1F02 ? t->gl_version : nullptr;
    if (v != nullptr) c_gl++;
    return (const unsigned char *) v;
}
static const unsigned char *h_gl1(unsigned int n) { const unsigned char *a = gl_answer(n); return a ? a : o_gl1(n); }
static const unsigned char *h_gl2(unsigned int n) { const unsigned char *a = gl_answer(n); return a ? a : o_gl2(n); }
static const unsigned char *h_gl3(unsigned int n) { const unsigned char *a = gl_answer(n); return a ? a : o_gl3(n); }

static std::string read_real(const char *path) {
    std::string out;
    int fd = (int) syscall(__NR_openat, AT_FDCWD, path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return out;
    char buf[1024];
    ssize_t n;
    while ((n = read(fd, buf, sizeof(buf))) > 0) out.append(buf, (size_t) n);
    close(fd);
    return out;
}
static void replace_all(std::string &s, const std::string &from, const std::string &to) {
    if (from.empty()) return;
    size_t p = 0;
    while ((p = s.find(from, p)) != std::string::npos) { s.replace(p, from.size(), to); p += to.size(); }
}
// /proc/version and the kernel sysctls follow the uname answer (real text, spoofed release/version)
static void derive_kernel_files(Table *t) {
    if (t->un_release.empty()) return;
    struct utsname real{};
    if (syscall(__NR_uname, &real) != 0) return;
    std::string pv = read_real("/proc/version");
    if (!pv.empty()) {
        replace_all(pv, real.release, t->un_release);
        replace_all(pv, real.version, t->un_version);
        t->files["/proc/version"] = pv;
    }
    t->files["/proc/sys/kernel/osrelease"] = t->un_release + "\n";
    t->files["/proc/sys/kernel/version"] = t->un_version + "\n";
}

// ---- popen("getprop ...") ----------------------------------------------------------------------
static std::mutex g_mem_mutex;
static std::unordered_map<FILE *, char *> g_mem_files;

static std::string rewrite_listing(const std::string &out) {
    std::string res;
    res.reserve(out.size());
    size_t start = 0, n = out.size();
    while (start < n) {
        size_t nl = out.find('\n', start);
        size_t end = nl == std::string::npos ? n : nl + 1;
        std::string line = out.substr(start, end - start);
        start = end;
        std::string body = line, eol;
        if (!body.empty() && body.back() == '\n') { eol = "\n"; body.pop_back(); }
        if (!body.empty() && body.back() == '\r') { eol = "\r" + eol; body.pop_back(); }
        size_t k = body.find("]: [");
        if (body.size() > 4 && body[0] == '[' && k != std::string::npos && k > 1 && body.back() == ']') {
            std::string key = body.substr(1, k - 1);
            std::string val = body.substr(k + 4, body.size() - k - 5);
            const std::string *r = val.empty() ? nullptr : rule_for(key.c_str());
            if (r != nullptr) {
                if (r->empty()) continue;
                res += "[" + key + "]: [" + *r + "]" + eol;
                continue;
            }
        }
        res += line;
    }
    return res;
}

// "getprop KEY [DEFAULT]" -> KEY, else "" (listing / real pipeline).
// 0.7.2: also accepts the wrappers detectors use around one getprop, e.g. VD Infos'
// "{ getprop KEY ; } 2>&1", "getprop KEY 2>/dev/null", "/system/bin/getprop KEY", "exec getprop KEY".
static bool is_noise_token(const std::string &t) {
    return t == "{" || t == "}" || t == ";" || t == "exec" || t == "2>&1" || t == "1>&2" ||
           t == "2>/dev/null" || t == ">/dev/null" || t == "</dev/null";
}

// one simple command with the usual wrappers removed, or false (pipelines, substitutions, lists)
static bool simple_command(const char *cmd, std::vector<std::string> *out) {
    if (strpbrk(cmd, "|`$\n") != nullptr) return false;
    if (strstr(cmd, "&&") != nullptr) return false;
    // tokenize on blanks; ';' is its own token
    std::vector<std::string> tok;
    std::string cur;
    char q = 0;
    for (const char *p = cmd;; p++) {
        char c = *p;
        if (c == 0 || (!q && (c == ' ' || c == '\t' || c == ';'))) {
            if (!cur.empty()) { tok.push_back(cur); cur.clear(); }
            if (c == ';') tok.emplace_back(";");
            if (c == 0) break;
            continue;
        }
        if (!q && (c == '\'' || c == '"')) { q = c; continue; }
        if (q && c == q) { q = 0; continue; }
        cur += c;
    }
    if (q) return false;
    for (auto &t : tok) if (!is_noise_token(t)) out->push_back(t);
    for (size_t i = 1; i < out->size(); i++)
        if ((*out)[i].find_first_of("><&{};") != std::string::npos) return false;
    return !out->empty();
}

static bool is_tool(const std::string &g, const char *name) {
    size_t n = strlen(name);
    return g == name || (g.size() > n && g.compare(g.size() - n - 1, n + 1, std::string("/") + name) == 0);
}

static std::string single_key(const char *cmd, std::string *def) {
    if (strstr(cmd, "getprop") == nullptr) return "";
    std::vector<std::string> rest;
    if (!simple_command(cmd, &rest)) return "";
    if (!is_tool(rest[0], "getprop")) return "";
    if (rest.size() < 2 || rest.size() > 3) return "";        // "getprop" alone = listing, more = several commands
    if (rest[1].empty() || rest[1][0] == '-') return "";
    if (def && rest.size() > 2) *def = rest[2];
    return rest[1];
}

static FILE *mem_file(const std::string &result, const char *cmd, const char *type);

static FILE *h_popen(const char *cmd, const char *type) {
    if (cmd == nullptr || type == nullptr || type[0] != 'r' || g_table.load(std::memory_order_acquire) == nullptr)
        return o_popen(cmd, type);
    // 0.7.4: "cat FILE" for an overridden file answers like open() does
    if (strstr(cmd, "cat") != nullptr) {
        std::vector<std::string> rest;
        if (simple_command(cmd, &rest) && rest.size() == 2 && is_tool(rest[0], "cat")) {
            const std::string *c = file_for(rest[1].c_str());
            if (c != nullptr) { c_file++; LOGD("popen %s -> override", cmd); return mem_file(*c, cmd, type); }
        }
    }
    // 0.7.9: awk '/^KEY/{print $N}' FILE on an overridden file (VD Infos: awk '/^MemTotal/{print $2}' /proc/meminfo)
    if (strstr(cmd, "awk") != nullptr) {
        static const std::regex re(R"RE(^\s*(?:\{\s*)?(?:exec\s+)?(?:\S*/)?awk\s+'/(\^?)([A-Za-z0-9_():.-]+)/\s*\{\s*print\s+\$([0-9]+)\s*\}'\s+(/[^\s;]+)\s*(?:;\s*\}\s*)?(?:2>&1|2>/dev/null)?\s*$)RE");
        std::cmatch m;
        if (std::regex_match(cmd, m, re)) {
            const std::string *c = file_for(m[4].str().c_str());
            if (c != nullptr) {
                bool anchor = m[1].length() > 0;
                std::string key = m[2].str(), o;
                int col = atoi(m[3].str().c_str());
                size_t st = 0;
                while (st < c->size()) {
                    size_t nl = c->find('\n', st);
                    size_t en = nl == std::string::npos ? c->size() : nl;
                    std::string line = c->substr(st, en - st);
                    st = en + 1;
                    bool hit = anchor ? line.compare(0, key.size(), key) == 0 : line.find(key) != std::string::npos;
                    if (!hit) continue;
                    if (col == 0) { o += line + "\n"; continue; }
                    // awk default field split: runs of blanks
                    int f = 0; size_t i = 0; std::string field;
                    while (i < line.size()) {
                        while (i < line.size() && (line[i] == ' ' || line[i] == '\t')) i++;
                        size_t j = i;
                        while (j < line.size() && line[j] != ' ' && line[j] != '\t') j++;
                        if (j > i && ++f == col) { field = line.substr(i, j - i); break; }
                        i = j;
                    }
                    o += field + "\n";
                }
                c_file++;
                LOGD("popen %s -> awk override", cmd);
                return mem_file(o, cmd, type);
            }
        }
    }
    // 0.7.8: "grep [-i|-F|-w|-m N] PATTERN FILE" on an overridden file (e.g. grep MemTotal /proc/meminfo)
    if (strstr(cmd, "grep") != nullptr) {
        std::vector<std::string> rest;
        if (simple_command(cmd, &rest) && rest.size() >= 3 && is_tool(rest[0], "grep")) {
            const std::string *c = file_for(rest.back().c_str());
            if (c != nullptr) {
                bool icase = false, ok = true;
                std::string pat;
                for (size_t i = 1; i + 1 < rest.size() && ok; i++) {
                    const std::string &a = rest[i];
                    if (a == "-i") icase = true;
                    else if (a == "-F" || a == "-w" || a == "-s") {}
                    else if (a == "-m" && i + 2 < rest.size()) i++;
                    else if (!a.empty() && a[0] == '-') ok = false;
                    else if (pat.empty()) pat = a;
                    else ok = false;
                }
                bool plain = pat.find_first_of(".*[]^$\\+?(){}|") == std::string::npos;
                if (ok && !pat.empty() && plain) {
                    std::string lp = pat, o;
                    if (icase) for (auto &ch : lp) ch = (char) tolower((unsigned char) ch);
                    size_t st = 0;
                    while (st < c->size()) {
                        size_t nl = c->find('\n', st);
                        size_t en = nl == std::string::npos ? c->size() : nl + 1;
                        std::string line = c->substr(st, en - st), cmp = line;
                        st = en;
                        if (icase) for (auto &ch : cmp) ch = (char) tolower((unsigned char) ch);
                        if (cmp.find(lp) != std::string::npos) { o += line; if (o.back() != '\n') o += '\n'; }
                    }
                    c_file++;
                    LOGD("popen %s -> grep override", cmd);
                    return mem_file(o, cmd, type);
                }
            }
        }
    }
    // 0.7.7: "uname [-asnrvm]" follows the uname() answer
    if (strstr(cmd, "uname") != nullptr) {
        Table *t = g_table.load(std::memory_order_acquire);
        std::vector<std::string> rest;
        if (t != nullptr && o_uname != nullptr && !t->un_release.empty() && simple_command(cmd, &rest) && !rest.empty() && is_tool(rest[0], "uname")) {
            struct utsname u{};
            if (h_uname(&u) == 0) {
                bool fs = false, fn = false, fr = false, fv = false, fm = false, ok = true;
                for (size_t i = 1; i < rest.size() && ok; i++) {
                    const std::string &a = rest[i];
                    if (a.size() < 2 || a[0] != '-') { ok = false; break; }
                    for (size_t j = 1; j < a.size(); j++) {
                        switch (a[j]) {
                            case 'a': fs = fn = fr = fv = fm = true; break;
                            case 's': fs = true; break;
                            case 'n': fn = true; break;
                            case 'r': fr = true; break;
                            case 'v': fv = true; break;
                            case 'm': fm = true; break;
                            default: ok = false;
                        }
                    }
                }
                if (ok) {
                    if (!(fs || fn || fr || fv || fm)) fs = true;
                    std::string o;
                    auto add = [&o](const char *f) { if (!o.empty()) o += ' '; o += f; };
                    if (fs) add(u.sysname);
                    if (fn) add(u.nodename);
                    if (fr) add(u.release);
                    if (fv) add(u.version);
                    if (fm) add(u.machine);
                    o += '\n';
                    LOGD("popen %s -> uname override", cmd);
                    return mem_file(o, cmd, type);
                }
            }
        }
    }
    if (strstr(cmd, "getprop") == nullptr) return o_popen(cmd, type);
    FILE *real = o_popen(cmd, type);
    if (real == nullptr) return real;
    std::string out;
    char buf[4096];
    size_t r;
    while ((r = fread(buf, 1, sizeof(buf), real)) > 0) out.append(buf, r);
    o_pclose(real);

    std::string def;
    std::string key = single_key(cmd, &def);
    std::string result;
    if (!key.empty()) {
        std::string realVal = out;
        while (!realVal.empty() && (realVal.back() == '\n' || realVal.back() == '\r')) realVal.pop_back();
        const std::string *rule = realVal.empty() ? nullptr : rule_for(key.c_str());
        if (rule == nullptr) result = out;
        else if (rule->empty()) result = def + "\n";
        else result = *rule + "\n";
    } else {
        result = rewrite_listing(out);
    }
    if (result != out) {
        c_popen++;
        LOGD("popen %s rewritten (%zu -> %zu bytes)", cmd, out.size(), result.size());
    }

    return mem_file(result, cmd, type);
}

static FILE *mem_file(const std::string &result, const char *cmd, const char *type) {
    char *mem = (char *) malloc(result.size() + 1);
    if (!mem) return o_popen(cmd, type);
    memcpy(mem, result.data(), result.size());
    mem[result.size()] = 0;
    FILE *f = fmemopen(mem, result.size() == 0 ? 1 : result.size(), "r");
    if (f == nullptr) { free(mem); return o_popen(cmd, type); }
    if (result.empty()) fseek(f, 0, SEEK_END);
    std::lock_guard<std::mutex> l(g_mem_mutex);
    g_mem_files[f] = mem;
    return f;
}

static int h_pclose(FILE *f) {
    char *mem = nullptr;
    {
        std::lock_guard<std::mutex> l(g_mem_mutex);
        auto it = g_mem_files.find(f);
        if (it != g_mem_files.end()) { mem = it->second; g_mem_files.erase(it); }
    }
    if (mem != nullptr) { fclose(f); free(mem); return 0; }
    return o_pclose(f);
}

// ---- install / uninstall -----------------------------------------------------------------------
static void hook_one(void *libc, const char *sym, void *repl, void **orig, void **slot) {
    void *target = dlsym(libc, sym);
    if (target == nullptr) { g_hook_errors += std::string(sym) + ":nosym "; return; }
    if (g_api->hookFunc(target, repl, orig) != 0 || *orig == nullptr) {
        g_hook_errors += std::string(sym) + ":fail ";
        *orig = nullptr;
        return;
    }
    *slot = target;
}

static bool install_hooks() {
    if (g_hooked.load()) return true;
    if (g_api == nullptr || g_api->hookFunc == nullptr) return false;
    void *libc = dlopen("libc.so", RTLD_NOW | RTLD_NOLOAD);
    if (libc == nullptr) { g_hook_errors = "libc:dlopen"; return false; }
    g_hook_errors.clear();
    // read_callback first: find/foreach rely on it for names
    hook_one(libc, "__system_property_read_callback", (void *) h_read_cb, (void **) &o_read_cb, &s_read_cb);
    hook_one(libc, "__system_property_get", (void *) h_get, (void **) &o_get, &s_get);
    hook_one(libc, "__system_property_find", (void *) h_find, (void **) &o_find, &s_find);
    hook_one(libc, "__system_property_read", (void *) h_read, (void **) &o_read, &s_read);
    if (o_read_cb != nullptr)
        hook_one(libc, "__system_property_foreach", (void *) h_foreach, (void **) &o_foreach, &s_foreach);
    hook_one(libc, "uname", (void *) h_uname, (void **) &o_uname, &s_uname);
    hook_one(libc, "open", (void *) h_open, (void **) &o_open, &s_open);
    hook_one(libc, "openat", (void *) h_openat, (void **) &o_openat, &s_openat);
    hook_one(libc, "__open_2", (void *) h_open2, (void **) &o_open2, &s_open2);
    hook_one(libc, "__openat_2", (void *) h_openat2, (void **) &o_openat2, &s_openat2);
    hook_one(libc, "fopen", (void *) h_fopen, (void **) &o_fopen, &s_fopen);
    // 0.7.7: GLES glGetString (libs come with libandroid_runtime, so NOLOAD finds them)
    {
        const char *libs[] = {"libGLESv1_CM.so", "libGLESv2.so", "libGLESv3.so"};
        glGetString_t *origs[] = {&o_gl1, &o_gl2, &o_gl3};
        void *repls[] = {(void *) h_gl1, (void *) h_gl2, (void *) h_gl3};
        void **slots[] = {&s_gl1, &s_gl2, &s_gl3};
        void *seen[3] = {nullptr, nullptr, nullptr};
        for (int i = 0; i < 3; i++) {
            void *h = dlopen(libs[i], RTLD_NOW | RTLD_NOLOAD);
            if (h == nullptr) { g_hook_errors += std::string(libs[i]) + ":notloaded "; continue; }
            void *sym = dlsym(h, "glGetString");
            bool dup = false;
            for (int k = 0; k < i; k++) if (seen[k] == sym) dup = true;
            seen[i] = sym;
            if (sym == nullptr || dup) continue;
            hook_one(h, "glGetString", repls[i], (void **) origs[i], slots[i]);
        }
    }
    hook_one(libc, "pclose", (void *) h_pclose, (void **) &o_pclose, &s_pclose);
    if (o_pclose != nullptr)
        hook_one(libc, "popen", (void *) h_popen, (void **) &o_popen, &s_popen);
    g_hooked = true;
    LOGI("hooks installed (api v%u) errors=[%s]", g_api->version, g_hook_errors.c_str());
    return true;
}

static void uninstall_hooks() {
    if (!g_hooked.load() || g_api == nullptr || g_api->unhookFunc == nullptr) return;
    void **slots[] = {&s_gl1, &s_gl2, &s_gl3, &s_popen, &s_pclose, &s_fopen, &s_openat2, &s_open2, &s_openat, &s_open, &s_uname,
                      &s_foreach, &s_read, &s_find, &s_get, &s_read_cb};
    for (void **s : slots) if (*s) { g_api->unhookFunc(*s); *s = nullptr; }
    g_hooked = false;
    LOGI("hooks removed");
}

extern "C" __attribute__((visibility("default"))) NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    g_api = entries;
    LOGI("native_init api v%u", entries ? entries->version : 0);
    return nullptr;
}

// ---- JNI: dev.identra.app.nativeprops.NativeProps ----------------------------------------------
static std::string jstr(JNIEnv *env, jstring s) {
    if (s == nullptr) return "";
    const char *c = env->GetStringUTFChars(s, nullptr);
    std::string r(c ? c : "");
    if (c) env->ReleaseStringUTFChars(s, c);
    return r;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_identra_app_nativeprops_NativeProps_nativeApiAvailable(JNIEnv *, jclass) {
    return g_api != nullptr && g_api->hookFunc != nullptr;
}

extern "C" JNIEXPORT jint JNICALL
Java_dev_identra_app_nativeprops_NativeProps_nativeApply(JNIEnv *env, jclass, jobjectArray keys, jobjectArray vals,
                                                      jobjectArray pfx, jobjectArray pfxVals, jboolean debug) {
    g_debug = debug;
    Table *t = new Table();
    jsize n = keys ? env->GetArrayLength(keys) : 0;
    for (jsize i = 0; i < n; i++) {
        jstring k = (jstring) env->GetObjectArrayElement(keys, i);
        jstring v = (jstring) env->GetObjectArrayElement(vals, i);
        std::string ks = jstr(env, k), vs = jstr(env, v);
        if (ks.rfind("@file:", 0) == 0) t->files[ks.substr(6)] = vs;
        else if (ks == "@uname:release") t->un_release = vs;
        else if (ks == "@uname:version") t->un_version = vs;
        else if (ks == "@uname:nodename") t->un_nodename = vs;
        else if (ks == "@gl:vendor") t->gl_vendor = strdup(vs.c_str());
        else if (ks == "@gl:renderer") t->gl_renderer = strdup(vs.c_str());
        else if (ks == "@gl:version") t->gl_version = strdup(vs.c_str());
        else t->exact[ks] = vs;
        env->DeleteLocalRef(k); env->DeleteLocalRef(v);
    }
    jsize m = pfx ? env->GetArrayLength(pfx) : 0;
    for (jsize i = 0; i < m; i++) {
        jstring k = (jstring) env->GetObjectArrayElement(pfx, i);
        jstring v = (jstring) env->GetObjectArrayElement(pfxVals, i);
        std::string ks = jstr(env, k);
        if (!ks.empty()) t->prefixes.push_back({ks, jstr(env, v)});
        env->DeleteLocalRef(k); env->DeleteLocalRef(v);
    }
    std::sort(t->prefixes.begin(), t->prefixes.end(),
              [](const Rule &a, const Rule &b) { return a.key.size() > b.key.size(); });
    if (t->un_version.empty()) t->un_release.clear();
    derive_kernel_files(t);
    if (!install_hooks()) { delete t; return -1; }
    g_table.store(t, std::memory_order_release);   // old table is leaked on purpose (readers may hold it)
    LOGI("rules applied: exact=%zu prefix=%zu files=%zu uname=%s", t->exact.size(), t->prefixes.size(), t->files.size(),
         t->un_release.empty() ? "-" : t->un_release.c_str());
    return (jint) (t->exact.size() + t->prefixes.size() + t->files.size() + (t->un_release.empty() ? 0 : 1));
}

extern "C" JNIEXPORT void JNICALL
Java_dev_identra_app_nativeprops_NativeProps_nativeRemove(JNIEnv *, jclass) {
    g_table.store(nullptr, std::memory_order_release);
    uninstall_hooks();
}

extern "C" JNIEXPORT jstring JNICALL
Java_dev_identra_app_nativeprops_NativeProps_nativeStatus(JNIEnv *env, jclass) {
    char buf[512];
    Table *t = g_table.load();
    snprintf(buf, sizeof(buf),
             "api=%s hooked=%d rules=%zu/%zu hits get=%u cb=%u read=%u findHidden=%u foreachHidden=%u popen=%u file=%u uname=%u gl=%u errors=[%s]",
             g_api ? std::to_string(g_api->version).c_str() : "none", g_hooked.load() ? 1 : 0,
             t ? t->exact.size() : 0, t ? t->prefixes.size() : 0,
             c_get.load(), c_cb.load(), c_read.load(), c_find_hidden.load(), c_foreach_hidden.load(), c_popen.load(), c_file.load(), c_uname.load(), c_gl.load(),
             g_hook_errors.c_str());
    return env->NewStringUTF(buf);
}

// probe through the (hooked) libc entry points, as an NDK app would see them
extern "C" JNIEXPORT jstring JNICALL
Java_dev_identra_app_nativeprops_NativeProps_nativeProbe(JNIEnv *env, jclass, jstring key) {
    std::string k = jstr(env, key);
    char v[PROP_VALUE_MAX] = {0};
    __system_property_get(k.c_str(), v);
    std::string out = std::string("get=") + v;
    const prop_info *pi = __system_property_find(k.c_str());
    if (pi == nullptr) out += " cb=<missing>";
    else {
        struct C { std::string v; } c;
        __system_property_read_callback(pi, [](void *ck, const char *, const char *val, uint32_t) {
            ((C *) ck)->v = val ? val : "";
        }, &c);
        out += " cb=" + c.v;
    }
    return env->NewStringUTF(out.c_str());
}
