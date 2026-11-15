package dev.identra.app.ui.profiles;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 0.4.0: "Keep separate data per profile". Saves an app's data directories into a root-only
 * store and restores another profile's copy, using {@code su} (KernelSU / Magisk).
 *
 * Store: /data/adb/identra/appdata/&lt;user&gt;/&lt;package&gt;/&lt;owner&gt;/{data,de,ext}.tar where
 * owner is a profile id or "original" (the data the app had before it was first switched).
 * Directories: /data/user/&lt;u&gt;/&lt;pkg&gt; (credential-encrypted, = /data/data for user 0),
 * /data/user_de/&lt;u&gt;/&lt;pkg&gt; (device-encrypted) and /data/media/&lt;u&gt;/Android/data/&lt;pkg&gt;
 * (= /sdcard/Android/data). OBB files and other users' copies are not touched.
 *
 * Safety: a save goes to a temp folder, every archive is listed back (tar -tf) and only then
 * the folder is moved into place; a restore runs only after the save succeeded, and only into
 * directories of a force-stopped app. Ownership is kept by tar (root extraction) and repaired
 * with chown when the app was reinstalled under a new uid; the SELinux context of each
 * directory is read before wiping and re-applied with chcon (restorecon as fallback).
 */
public final class AppDataVault {
    private static final String TAG = "Identra.AppDataVault";
    public static final String STORE = "/data/adb/identra/appdata";
    private static final String[] TAGS = {"data", "de", "ext"};

    private static Boolean rootCache;

    private AppDataVault() { }

    public static final class Result {
        public final boolean ok;
        public final String output;
        public Result(boolean ok, String output) { this.ok = ok; this.output = output; }
    }

    /** Blocking: asks su once (KernelSU may show its grant prompt). */
    public static synchronized boolean hasRoot(boolean recheck) {
        if (rootCache != null && !recheck) return rootCache;
        Result r = run("id", 15);
        rootCache = r.ok && r.output.contains("uid=0");
        return rootCache;
    }

    public static Boolean cachedRoot() { return rootCache; }

    public static boolean safeName(String s) { return s != null && s.matches("[A-Za-z0-9_][A-Za-z0-9_.\\-]{0,200}"); }

    private static String dirsList(String pkg, int userId) {
        String data = userId == 0 ? "/data/data/" + pkg : "/data/user/" + userId + "/" + pkg;
        return "data:" + data + " de:/data/user_de/" + userId + "/" + pkg + " ext:/data/media/" + userId + "/Android/data/" + pkg;
    }

    private static String storeDir(String pkg, int userId, String owner) {
        return STORE + "/" + userId + "/" + pkg + "/" + owner;
    }

    public static boolean has(String pkg, int userId, String owner) {
        if (!safeName(pkg) || !safeName(owner)) return false;
        Result r = run("[ -d '" + storeDir(pkg, userId, owner) + "' ] && echo HAS_STORE", 20);
        return r.output.contains("HAS_STORE");
    }

    /** Copies the app's current data into the store under {@code owner}. App must be stopped. */
    public static Result save(String pkg, int userId, String owner) {
        if (!safeName(pkg) || !safeName(owner)) return new Result(false, "invalid name");
        String s = storeDir(pkg, userId, owner);
        String script = "S='" + s + "'\n"
                + "T=\"$S.tmp\"\n"
                + "rm -rf \"$T\"; mkdir -p \"$T\" || { echo 'ERR mkdir'; exit 11; }\n"
                + "for pair in " + dirsList(pkg, userId) + "; do\n"
                + "  tag=${pair%%:*}; dir=${pair#*:}\n"
                + "  if [ -d \"$dir\" ]; then\n"
                + "    tar -cf \"$T/$tag.tar\" -C \"$dir\" . || { echo \"ERR tar $tag\"; rm -rf \"$T\"; exit 12; }\n"
                + "    tar -tf \"$T/$tag.tar\" > /dev/null || { echo \"ERR verify $tag\"; rm -rf \"$T\"; exit 13; }\n"
                + "  fi\n"
                + "done\n"
                + "date +%s > \"$T/saved_at\"\n"
                + "rm -rf \"$S.old\"\n"
                + "if [ -d \"$S\" ]; then mv \"$S\" \"$S.old\" || { echo 'ERR rotate'; rm -rf \"$T\"; exit 14; }; fi\n"
                + "mv \"$T\" \"$S\" || { echo 'ERR commit'; [ -d \"$S.old\" ] && mv \"$S.old\" \"$S\"; exit 15; }\n"
                + "rm -rf \"$S.old\"\n"
                + "chmod -R 700 '" + STORE + "'\n"
                + "echo OK_SAVED\n";
        Result r = run(script, 900);
        boolean ok = r.ok && r.output.contains("OK_SAVED");
        Log.i(TAG, "save " + pkg + " u" + userId + " as " + owner + " ok=" + ok);
        return new Result(ok, r.output);
    }

    /**
     * Replaces the app's data with the copy saved under {@code owner}; when there is no copy,
     * empties the directories (fresh start). Only call after a successful {@link #save}.
     */
    public static Result restoreOrFresh(String pkg, int userId, String owner) {
        if (!safeName(pkg) || !safeName(owner)) return new Result(false, "invalid name");
        String s = storeDir(pkg, userId, owner);
        String script = "S='" + s + "'\n"
                + "HAVE=0; [ -d \"$S\" ] && HAVE=1\n"
                + "for pair in " + dirsList(pkg, userId) + "; do\n"
                + "  tag=${pair%%:*}; dir=${pair#*:}\n"
                + "  own=''; ctx=''\n"
                + "  if [ -d \"$dir\" ]; then\n"
                + "    own=$(stat -c %u:%g \"$dir\"); ctx=$(stat -c %C \"$dir\" 2>/dev/null)\n"
                + "    find \"$dir\" -mindepth 1 -delete || { echo \"ERR wipe $tag\"; exit 22; }\n"
                + "  fi\n"
                + "  if [ $HAVE = 1 ] && [ -f \"$S/$tag.tar\" ]; then\n"
                + "    mkdir -p \"$dir\"\n"
                + "    tar -xpf \"$S/$tag.tar\" -C \"$dir\" || { echo \"ERR extract $tag\"; exit 23; }\n"
                + "    if [ -n \"$own\" ] && [ \"$(stat -c %u:%g \"$dir\")\" != \"$own\" ]; then chown -R \"$own\" \"$dir\"; fi\n"
                + "    if [ -n \"$ctx\" ] && [ \"$ctx\" != '?' ]; then chcon -R \"$ctx\" \"$dir\" 2>/dev/null || restorecon -RF \"$dir\"; else restorecon -RF \"$dir\" 2>/dev/null; fi\n"
                + "  fi\n"
                + "done\n"
                + "[ $HAVE = 1 ] && echo OK_RESTORED || echo OK_FRESH\n";
        Result r = run(script, 900);
        boolean ok = r.ok && (r.output.contains("OK_RESTORED") || r.output.contains("OK_FRESH"));
        Log.i(TAG, "restore " + pkg + " u" + userId + " from " + owner + " ok=" + ok);
        return new Result(ok, r.output);
    }

    public static Result forceStop(String pkg, int userId) {
        if (!safeName(pkg)) return new Result(false, "invalid name");
        return run("am force-stop --user " + userId + " " + pkg + "; echo OK_STOPPED", 30);
    }

    /** Size of the saved copies for a package, for the UI ("12 MB saved"). */
    public static String storedSize(String pkg, int userId) {
        if (!safeName(pkg)) return null;
        Result r = run("du -sh '" + STORE + "/" + userId + "/" + pkg + "' 2>/dev/null", 30);
        if (!r.ok || r.output.trim().isEmpty()) return null;
        return r.output.trim().split("\\s+")[0];
    }

    /** Runs a shell script as root; never throws. */
    public static Result run(String script, long timeoutSec) {
        Process p = null;
        try {
            p = new ProcessBuilder("su").redirectErrorStream(true).start();
            try (OutputStream os = p.getOutputStream()) {
                os.write((script + "\nexit\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            final StringBuilder out = new StringBuilder();
            final Process fp = p;
            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(fp.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        synchronized (out) { if (out.length() < 16000) out.append(line).append('\n'); }
                    }
                } catch (Throwable ignored) { }
            });
            reader.start();
            boolean finished = p.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                p.destroy();
                return new Result(false, "timeout");
            }
            reader.join(2000);
            String o;
            synchronized (out) { o = out.toString(); }
            return new Result(p.exitValue() == 0, o);
        } catch (Throwable t) {
            if (p != null) try { p.destroy(); } catch (Throwable ignored) { }
            return new Result(false, String.valueOf(t));
        }
    }
}
