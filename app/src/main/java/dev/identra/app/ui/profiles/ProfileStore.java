package dev.identra.app.ui.profiles;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 0.4.0: profiles and their assignments, kept in one JSON file in the app's files dir
 * (files/identra_profiles.json, written atomically through a temp file). The module database
 * only ever sees the per-app setting values a profile writes; this file is the record of
 * which profile each app uses and how its data is handled.
 */
public final class ProfileStore {
    private static final String TAG = "Identra.Profiles";
    public static final String FILE = "identra_profiles.json";

    public static final String DATA_SHARED = "shared", DATA_FRESH = "fresh", DATA_SEPARATE = "separate";
    /** Data owner marker for an app's data from before it was ever switched with "keep separate". */
    public static final String ORIGINAL = "original";

    public static final class Assignment {
        public String pkg;
        public int uid;
        public String profileId;
        public String dataMode = DATA_SHARED;
        /** Whose data the app's directories currently hold (separate mode only). */
        public String dataOwner;
        public long applied;

        public String key() { return keyOf(pkg, uid); }
    }

    private static ProfileStore instance;

    public static synchronized ProfileStore get(Context c) {
        if (instance == null) instance = new ProfileStore(c.getApplicationContext());
        return instance;
    }

    private final File file;
    private final Map<String, Profile> profiles = new LinkedHashMap<>();
    private final Map<String, Assignment> assignments = new LinkedHashMap<>();
    private String globalId;

    private ProfileStore(Context c) {
        file = new File(c.getFilesDir(), FILE);
        load();
    }

    public static String keyOf(String pkg, int uid) { return pkg + ":" + uid; }

    private synchronized void load() {
        profiles.clear();
        assignments.clear();
        globalId = null;
        if (!file.isFile()) return;
        try {
            String text = new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(text);
            JSONArray arr = root.optJSONArray("profiles");
            if (arr != null) for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Profile p = Profile.fromJson(o);
                profiles.put(p.id, p);
            }
            JSONObject as = root.optJSONObject("assignments");
            if (as != null) {
                Iterator<String> it = as.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    JSONObject o = as.optJSONObject(k);
                    if (o == null) continue;
                    Assignment a = new Assignment();
                    a.pkg = o.optString("pkg");
                    a.uid = o.optInt("uid");
                    a.profileId = o.optString("profile", null);
                    a.dataMode = o.optString("data", DATA_SHARED);
                    a.dataOwner = o.optString("owner", null);
                    a.applied = o.optLong("applied");
                    if (a.pkg.isEmpty()) continue;
                    assignments.put(a.key(), a);
                }
            }
            globalId = root.optString("global", null);
            if (globalId != null && globalId.isEmpty()) globalId = null;
        } catch (Throwable t) {
            Log.e(TAG, "Profiles file unreadable, keeping a copy: " + t);
            //noinspection ResultOfMethodCallIgnored
            file.renameTo(new File(file.getParentFile(), FILE + ".broken-" + System.currentTimeMillis()));
        }
    }

    public synchronized boolean save() {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            JSONArray arr = new JSONArray();
            for (Profile p : profiles.values()) arr.put(p.toJson());
            root.put("profiles", arr);
            JSONObject as = new JSONObject();
            for (Assignment a : assignments.values()) {
                JSONObject o = new JSONObject();
                o.put("pkg", a.pkg);
                o.put("uid", a.uid);
                if (a.profileId != null) o.put("profile", a.profileId);
                o.put("data", a.dataMode == null ? DATA_SHARED : a.dataMode);
                if (a.dataOwner != null) o.put("owner", a.dataOwner);
                o.put("applied", a.applied);
                as.put(a.key(), o);
            }
            root.put("assignments", as);
            if (globalId != null) root.put("global", globalId);
            File tmp = new File(file.getParentFile(), FILE + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(root.toString(1).getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            if (!tmp.renameTo(file)) throw new IllegalStateException("rename failed");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Save failed: " + t);
            return false;
        }
    }

    // ---- profiles ----

    public synchronized List<Profile> list() {
        List<Profile> l = new ArrayList<>();
        for (Profile p : profiles.values()) if (!p.temp) l.add(p);
        Collections.sort(l, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return l;
    }

    public synchronized Profile byId(String id) { return id == null ? null : profiles.get(id); }

    public synchronized int count() { return list().size(); }

    /** Deletes an unsaved (temp) profile once no app uses it. */
    public synchronized void dropIfOrphan(Profile p) {
        if (p != null && p.temp && usersOf(p.id).isEmpty()) delete(p.id);
    }

    public synchronized void put(Profile p) {
        p.updated = System.currentTimeMillis();
        profiles.put(p.id, p);
        save();
    }

    /** Removes the profile and its assignment records (callers clear per-app values first). */
    public synchronized void delete(String id) {
        profiles.remove(id);
        for (Assignment a : new ArrayList<>(assignments.values()))
            if (id.equals(a.profileId)) {
                // keep the data-owner record for apps whose data lives in the vault
                if (a.dataOwner != null) a.profileId = null;
                else assignments.remove(a.key());
            }
        if (id.equals(globalId)) globalId = null;
        save();
    }

    public synchronized String uniqueName(String base) {
        String name = base;
        int n = 2;
        while (nameTaken(name)) name = base + " " + (n++);
        return name;
    }

    private boolean nameTaken(String name) {
        for (Profile p : profiles.values()) if (p.name.equalsIgnoreCase(name)) return true;
        return false;
    }

    // ---- assignments ----

    public synchronized Assignment assignment(String pkg, int uid) {
        Assignment a = assignments.get(keyOf(pkg, uid));
        return a == null || a.profileId == null ? null : a;
    }

    /** Record including a profile-less data-owner entry. */
    public synchronized Assignment record(String pkg, int uid) { return assignments.get(keyOf(pkg, uid)); }

    public synchronized Profile profileFor(String pkg, int uid) {
        Assignment a = assignment(pkg, uid);
        return a == null ? null : profiles.get(a.profileId);
    }

    public synchronized void setAssignment(Assignment a) {
        a.applied = System.currentTimeMillis();
        assignments.put(a.key(), a);
        save();
    }

    public synchronized void clearAssignment(String pkg, int uid) {
        Assignment a = assignments.get(keyOf(pkg, uid));
        if (a == null) return;
        if (a.dataOwner != null) a.profileId = null;   // remember whose data is live
        else assignments.remove(a.key());
        save();
    }

    public synchronized List<Assignment> usersOf(String profileId) {
        List<Assignment> l = new ArrayList<>();
        for (Assignment a : assignments.values()) if (profileId.equals(a.profileId)) l.add(a);
        return l;
    }

    public synchronized List<Assignment> allAssignments() {
        List<Assignment> l = new ArrayList<>();
        for (Assignment a : assignments.values()) if (a.profileId != null) l.add(a);
        return l;
    }

    // ---- global default ----

    public synchronized String globalId() { return globalId != null && profiles.containsKey(globalId) ? globalId : null; }

    public synchronized Profile global() { return byId(globalId()); }

    public synchronized void setGlobal(String id) { globalId = id; save(); }

    // ---- import / export ----

    public static String export(List<Profile> list) throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", "identra-profiles");
        root.put("version", 1);
        JSONArray arr = new JSONArray();
        for (Profile p : list) arr.put(p.toJson());
        root.put("profiles", arr);
        return root.toString(2);
    }

    /** Accepts an export file, a bare array, or one profile object. Returns the imported profiles (new ids). */
    public synchronized List<Profile> importJson(String text) throws Exception {
        List<JSONObject> objs = new ArrayList<>();
        String t = text.trim();
        if (t.startsWith("[")) {
            JSONArray a = new JSONArray(t);
            for (int i = 0; i < a.length(); i++) if (a.optJSONObject(i) != null) objs.add(a.optJSONObject(i));
        } else {
            JSONObject o = new JSONObject(t);
            JSONArray a = o.optJSONArray("profiles");
            if (a != null) { for (int i = 0; i < a.length(); i++) if (a.optJSONObject(i) != null) objs.add(a.optJSONObject(i)); }
            else objs.add(o);
        }
        List<Profile> out = new ArrayList<>();
        for (JSONObject o : objs) {
            if (o.optJSONObject("values") == null) continue;
            Profile p = Profile.fromJson(o);
            p.id = Profile.newId();
            p.name = uniqueName(p.name == null || p.name.trim().isEmpty() ? "Imported" : p.name.trim());
            // keep only keys this version knows
            for (String k : new ArrayList<>(p.values.keySet())) if (!ProfileKeys.contains(k)) p.values.remove(k);
            p.locked.retainAll(p.values.keySet());
            p.updated = System.currentTimeMillis();
            profiles.put(p.id, p);
            out.add(p);
        }
        if (out.isEmpty()) throw new IllegalArgumentException("no profiles in file");
        save();
        return out;
    }
}
