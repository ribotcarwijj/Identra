package dev.identra.app.found;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 0.6.0 "Found in this app": what every Identra hook looks for, in a shape both detectors can match
 * quickly. Pure Java (no Android resources), so the same index works in the Identra app (static dex
 * scan), inside hooked app processes (observe mode) and in system_server (auto-assign policy).
 * <p>
 * Built once from {@link FoundIndexData} (generated from assets/**&#47;hooks.json by
 * tools/gen_found_index.py) and cached.
 */
public final class FoundIndex {

    /** Setting (per app) with the hook ids seen firing in the app, comma separated; written by system_server. */
    public static final String SETTING_SEEN = "identra.found.seen";
    /** Setting (per app) "true" while "Use identity for everything found" is on: system_server assigns new detections. */
    public static final String SETTING_AUTO = "identra.found.auto";
    /** Setting (per app) with the hook ids system_server assigned on its own while auto was on. */
    public static final String SETTING_AUTO_ADDED = "identra.found.autoadded";
    /** Global setting; "false" turns observe mode off everywhere (default on). */
    public static final String SETTING_OBSERVE = "identra.found.observe";

    public static final class Entry {
        public final String id, group, className, methodName, returnType;
        public final String[] params, settings;

        Entry(String[] f) {
            id = f[0];
            group = f[1];
            className = f[2];
            methodName = f[3];
            params = split(f[4]);
            settings = split(f[5]);
            returnType = f.length > 6 ? f[6] : "";
        }

        public boolean isRule() { return className.startsWith("Intercept."); }
        public boolean isField() { return methodName.startsWith("#"); }
    }

    /** Key match tables for one kind of key (property names, settings keys, ...). */
    public static final class KeyTable {
        public final Map<String, List<String>> exact = new HashMap<>();
        /** prefix (pattern without trailing '*') -> ids */
        public final Map<String, List<String>> prefix = new HashMap<>();

        void add(String key, String id) {
            if (key == null) return;
            key = key.trim();
            if (key.isEmpty() || key.equals("*")) return;
            if (key.endsWith("*")) put(prefix, key.substring(0, key.length() - 1), id);
            else put(exact, key, id);
        }

        /** Ids whose key matches; adds to out. */
        public void match(String key, Set<String> out) {
            if (key == null) return;
            List<String> l = exact.get(key);
            if (l != null) out.addAll(l);
            if (!prefix.isEmpty())
                for (Map.Entry<String, List<String>> e : prefix.entrySet())
                    if (key.startsWith(e.getKey())) out.addAll(e.getValue());
        }

        public boolean isEmpty() { return exact.isEmpty() && prefix.isEmpty(); }
    }

    public final List<Entry> entries = new ArrayList<>();
    public final Map<String, Entry> byId = new HashMap<>();
    /** class name -> method name -> ids. Method "*" = any method of the class. Fields as "#NAME". */
    public final Map<String, Map<String, List<String>>> members = new HashMap<>();
    /** System property names (SystemProperties.get, getprop). */
    public final KeyTable props = new KeyTable();
    /** Settings.Secure/Global/System names (Settings.*.getString, settings provider calls). */
    public final KeyTable settingsKeys = new KeyTable();
    /** Shell command words (Intercept.Shell rules, e.g. "stat", "meminfo"). */
    public final Map<String, List<String>> shellWords = new HashMap<>();
    /** Plain strings that indicate a hook in a dex string table (AIDL descriptors, provider authorities, file paths). */
    public final Map<String, List<String>> markers = new HashMap<>();

    private static volatile FoundIndex instance;

    public static FoundIndex get() {
        FoundIndex i = instance;
        if (i == null) {
            synchronized (FoundIndex.class) {
                if (instance == null) instance = new FoundIndex();
                i = instance;
            }
        }
        return i;
    }

    private FoundIndex() {
        for (String row : FoundIndexData.ROWS) {
            String[] f = row.split("\t", -1);
            if (f.length < 6) continue;
            Entry e = new Entry(f);
            if (!isDetectable(e.group)) continue;
            entries.add(e);
            byId.put(e.id, e);
            index(e);
        }
    }

    private void index(Entry e) {
        String cls = e.className;
        String m = e.methodName;
        if (cls.equals("Intercept.Properties")) {
            for (String k : e.params) props.add(k, e.id);
        } else if (cls.equals("Intercept.Settings.Call")) {
            for (String k : e.params) settingsKeys.add(k, e.id);
        } else if (cls.equals("Intercept.Intent.Query")) {
            // methodName is the provider ("settings", an authority, or several joined by '|')
            if (m.equals("settings") || m.equals("*")) {
                for (String k : e.params) settingsKeys.add(k, e.id);
            } else {
                for (String a : m.split("\\|")) {
                    String auth = a.contains("/") ? a.substring(0, a.indexOf('/')) : a;
                    marker(auth, e.id);
                }
            }
        } else if (cls.equals("Intercept.Shell")) {
            put(shellWords, m.toLowerCase(Locale.ROOT), e.id);
        } else if (cls.equals("Intercept.IPC.Interface")) {
            marker(m, e.id);                       // AIDL interface descriptor
        } else if (cls.equals("Intercept.File")) {
            for (String p : e.params) {
                String s = p.replace("*", "");
                if (s.length() >= 6) marker(s, e.id);
            }
        } else if (!cls.startsWith("Intercept.")) {
            for (String c : cls.split("\\|")) {
                c = c.trim();
                if (c.isEmpty()) continue;
                String key;
                if (m.contains(":")) key = "#" + m.substring(0, m.indexOf(':'));   // e.g. CREATOR:createFromParcel
                else key = m;                                                     // name, "#FIELD" or "*"
                Map<String, List<String>> byName = members.get(c);
                if (byName == null) { byName = new HashMap<>(); members.put(c, byName); }
                put(byName, key, e.id);
            }
        }
    }

    private void marker(String s, String id) {
        if (s == null) return;
        s = s.trim();
        if (s.length() < 4) return;
        put(markers, s, id);
    }

    // ---- policy shared by the app, the observer and system_server ----

    /** Infrastructure groups (base intercepts, tests, profile helpers) never show up as "found". */
    public static boolean isDetectable(String group) {
        if (group == null || group.isEmpty()) return false;
        String g = group.toLowerCase(Locale.ROOT);
        return !(g.startsWith("intercept.") || g.startsWith("test.") || g.startsWith("profile."));
    }

    /**
     * Groups "Use identity for everything found" assigns: hooks that answer with another identity
     * (device, IDs, SIM/operator values, network info, location, language/time, accounts, settings,
     * sensors, display, storage, installed-app lists). Pure privacy blocks that make data disappear
     * (contacts, messages, call log, calendar, clipboard, notifications, camera, microphone, sending
     * SMS) and analytics blockers are listed as found but never switched on automatically, because
     * they change what the app can do, not who the phone is.
     */
    public static boolean isIdentity(String group) {
        if (!isDetectable(group)) return false;
        String g = group.toLowerCase(Locale.ROOT);
        for (String p : NOT_IDENTITY) if (g.startsWith(p)) return false;
        return true;
    }

    private static final String[] NOT_IDENTITY = {
            "get.contacts", "get.messages", "get.call.log", "get.calendars", "get.clipboard", "get.activity",
            "read.notifications", "read.sync", "record.", "use.camera", "send.messages", "ad.analytics", "ad.",
            "hide.environment", "storage.isolate", "spoof.embed.", "spoof.contacts"
    };

    // ---- helpers ----

    static String[] split(String s) {
        if (s == null || s.isEmpty()) return new String[0];
        return s.split(",");
    }

    static void put(Map<String, List<String>> map, String key, String id) {
        List<String> l = map.get(key);
        if (l == null) { l = new ArrayList<>(2); map.put(key, l); }
        if (!l.contains(id)) l.add(id);
    }

    /** Parses a comma separated id list (setting value) into a set. */
    public static Set<String> parseIds(String value) {
        if (value == null || value.trim().isEmpty()) return new HashSet<>();
        Set<String> s = new HashSet<>();
        for (String p : value.split(",")) {
            p = p.trim();
            if (!p.isEmpty()) s.add(p);
        }
        return s;
    }

    public static String joinIds(Set<String> ids) {
        List<String> l = new ArrayList<>(ids);
        Collections.sort(l);
        StringBuilder sb = new StringBuilder();
        for (String s : l) { if (sb.length() > 0) sb.append(','); sb.append(s); }
        return sb.toString();
    }
}
