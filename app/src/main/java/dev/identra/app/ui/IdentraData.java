package dev.identra.app.ui;

import android.content.Context;
import android.content.res.Resources;
import android.os.Process;
import android.util.Log;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.call.AssignHooksCommand;
import dev.identra.app.x.core.commands.call.GetSettingExCommand;
import dev.identra.app.x.core.commands.query.GetAppsCommand;
import dev.identra.app.x.core.commands.query.GetHooksCommand;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.core.hook.AssignmentPacket;
import dev.identra.app.x.core.hook.AssignmentsPacket;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;

/**
 * Data layer for the new UI. Uses exactly the same commands as the classic
 * app list (GetSettingExCommand.getCollections, GetHooksCommand.getHooks,
 * GetAppsCommand.get, AssignHooksCommand.call with AssignmentsPacket).
 */
public final class IdentraData {
    private static final String TAG = "Identra.UiData";

    private IdentraData() { }

    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static final class Snapshot {
        public final List<String> collections = new ArrayList<>();
        /** Hooks as the classic list uses them (valid group, enabled or special filter). */
        public final List<XHook> hooks = new ArrayList<>();
        public final Map<String, XHook> hookById = new HashMap<>();
        public final List<AppXpPacket> apps = new ArrayList<>();
        /** True when a local change happened while this snapshot was loading; load again. */
        public boolean stale = false;
    }

    private static volatile int mutations = 0;

    private static volatile Snapshot cache;

    public static Snapshot cached() { return cache; }

    /** Blocking; call off the main thread. */
    public static Snapshot load(Context context) {
        final int startMutations = mutations;
        Snapshot s = new Snapshot();
        List<String> collections = GetSettingExCommand.getCollections(context, Process.myUid());
        if (collections != null) s.collections.addAll(collections);

        List<XHook> hooks = GetHooksCommand.getHooks(context, true, false);
        if (hooks != null) {
            for (XHook h : hooks) {
                if (h == null || Str.isEmpty(h.getObjectId())) continue;
                s.hookById.put(h.getObjectId(), h);
                if (!Str.isEmpty(h.group)) s.hooks.add(h);
            }
        }

        List<AppXpPacket> apps = GetAppsCommand.get(context, true);
        if (apps != null) {
            Map<Integer, String> inst = instanceNumbers(context);
            Map<String, AppXpPacket> owner = new HashMap<>();
            for (AppXpPacket a : apps) if (a != null && a.packageName != null) {
                markInstance(a, inst);
                s.apps.add(a);
                if (a.uid / 100000 == 0) owner.put(a.packageName, a);
            }
            // 0.7.25: each app copy inside an instance is its own row, with its own protection.
            for (Map.Entry<Integer, List<String>> e : instanceApps(context).entrySet()) {
                for (String pkg : e.getValue()) {
                    AppXpPacket base = owner.get(pkg);
                    if (base == null) continue;
                    AppXpPacket c = new AppXpPacket();
                    c.packageName = pkg;
                    c.uid = e.getKey() * 100000 + base.uid % 100000;
                    c.icon = base.icon;
                    c.label = base.label;
                    c.enabled = base.enabled;
                    c.system = base.system;
                    c.persistent = base.persistent;
                    c.forceStop = base.forceStop;
                    try {
                        Collection<XHook> hs = dev.identra.app.x.core.commands.query.GetAssignedHooksExCommand.get(context, true, c.uid, pkg);
                        if (hs != null) for (XHook h : hs) if (h != null) c.addAssignment(AssignmentPacket.create(h));
                    } catch (Throwable t) { Log.w(TAG, "clone assignments " + pkg + "/" + c.uid + ": " + t); }
                    markInstance(c, inst);
                    s.apps.add(c);
                }
            }
        }
        final Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.SECONDARY);
        Collections.sort(s.apps, (a, b) -> collator.compare(a.label == null ? a.packageName : a.label, b.label == null ? b.packageName : b.label));

        Log.i(TAG, "Loaded collections=" + s.collections + " hooks=" + s.hooks.size() + " apps=" + s.apps.size());
        s.stale = startMutations != mutations;
        // A stale snapshot misses a local change made while loading: keep the current cache.
        if (!s.stale || cache == null) cache = s;
        return s;
    }

    public static AppXpPacket findApp(Snapshot s, String packageName, int uid) {
        if (s == null || packageName == null) return null;
        for (AppXpPacket a : s.apps)
            if (packageName.equals(a.packageName) && a.uid == uid) return a;
        return null;
    }

    // ---- assignment state -------------------------------------------------

    public static Set<String> assignedIds(AppXpPacket app) {
        Set<String> ids = new LinkedHashSet<>();
        if (app == null) return ids;
        synchronized (app.assignments) {
            for (AssignmentPacket a : app.assignments) {
                if (a == null) continue;
                String id = a.getHookId();
                if (!Str.isEmpty(id)) ids.add(id);
            }
        }
        return ids;
    }

    public static String groupOfAssignment(Snapshot s, AssignmentPacket a) {
        if (a == null) return null;
        String id = a.getHookId();
        XHook h = (s == null || id == null) ? null : s.hookById.get(id);
        if (h != null && !Str.isEmpty(h.group)) return h.group;
        if (a.hookObj != null && !Str.isEmpty(a.hookObj.group)) return a.hookObj.group;
        return null;
    }

    /** Distinct groups the app has at least one assignment in (known hooks only). */
    public static Set<String> assignedGroups(Snapshot s, AppXpPacket app) {
        Set<String> groups = new LinkedHashSet<>();
        if (app == null) return groups;
        synchronized (app.assignments) {
            for (AssignmentPacket a : app.assignments) {
                String g = groupOfAssignment(s, a);
                if (g != null) groups.add(g);
            }
        }
        return groups;
    }

    public static boolean isProtected(AppXpPacket app) {
        return app != null && !app.assignments.isEmpty();
    }

    /** All hooks available to this app in the active collections (what the classic group list shows). */
    public static List<XHook> availableHooks(Snapshot s, AppXpPacket app) {
        List<XHook> out = new ArrayList<>();
        if (s == null || app == null) return out;
        for (XHook h : s.hooks)
            if (h.isAvailable(app.packageName, s.collections)) out.add(h);
        return out;
    }

    /**
     * The recommended default set for the main switch. Mirrors the classic app list
     * checkbox (all available hooks of the active collections, excluding Intercept.*
     * and disabled hooks, exactly as FragmentMain filters them), restricted to the
     * Identra collection.
     */
    public static List<XHook> defaultHooks(Snapshot s, AppXpPacket app) {
        List<XHook> out = new ArrayList<>();
        if (s == null || app == null) return out;
        boolean coreActive = false;
        for (String c : s.collections)
            if (GetSettingExCommand.SETTING_COLLECTION_DEFAULT.equalsIgnoreCase(c)) coreActive = true;
        for (XHook h : availableHooks(s, app)) {
            if (Str.isEmpty(h.group)) continue;
            if (h.group.toLowerCase(Locale.ROOT).startsWith("intercept.")) continue;
            // 0.5.0: profile-driven groups (vendor property hiding) are never part of the default set
            if (h.group.toLowerCase(Locale.ROOT).startsWith("profile.")) continue;
            if (h.enabled != null && !Boolean.TRUE.equals(h.enabled)) continue;
            if (coreActive && !GetSettingExCommand.SETTING_COLLECTION_DEFAULT.equalsIgnoreCase(h.collection)) continue;
            out.add(h);
        }
        return out;
    }

    /** Available hooks grouped by group name, in stable order. */
    public static Map<String, List<XHook>> groupsOf(List<XHook> hooks) {
        Map<String, List<XHook>> map = new LinkedHashMap<>();
        for (XHook h : hooks) {
            List<XHook> l = map.get(h.group);
            if (l == null) { l = new ArrayList<>(); map.put(h.group, l); }
            l.add(h);
        }
        return map;
    }

    // ---- mutations (update the local model immediately, push in background) ----

    public interface Done { void onDone(A_CODE code); }

    /** Assign the given hooks (only those not yet assigned). Returns false if nothing to do. */
    public static boolean assign(Context context, AppXpPacket app, Collection<XHook> hooks, Done done) {
        Set<String> have = assignedIds(app);
        final ArrayList<String> ids = new ArrayList<>();
        synchronized (app.assignments) {
            for (XHook h : hooks) {
                String id = h.getObjectId();
                if (Str.isEmpty(id) || have.contains(id) || ids.contains(id)) continue;
                ids.add(id);
                app.addAssignment(AssignmentPacket.create(h));
            }
        }
        if (ids.isEmpty()) return false;
        push(context, app, ids, false, done);
        return true;
    }

    /** Remove the given hook ids (only those assigned). */
    public static boolean unassign(Context context, AppXpPacket app, Collection<String> hookIds, Done done) {
        Set<String> target = new HashSet<>(hookIds);
        final ArrayList<String> ids = new ArrayList<>();
        synchronized (app.assignments) {
            List<AssignmentPacket> keep = new ArrayList<>();
            for (AssignmentPacket a : app.assignments) {
                String id = a == null ? null : a.getHookId();
                if (id != null && target.contains(id)) {
                    if (!ids.contains(id)) ids.add(id);
                } else if (a != null) {
                    keep.add(a);
                }
            }
            app.assignments.clear();
            app.assignments.addAll(keep);
        }
        if (ids.isEmpty()) return false;
        push(context, app, ids, true, done);
        return true;
    }

    public static boolean unassignAll(Context context, AppXpPacket app, Done done) {
        return unassign(context, app, assignedIds(app), done);
    }

    public static List<String> idsOf(Collection<XHook> hooks) {
        List<String> ids = new ArrayList<>();
        for (XHook h : hooks) if (!Str.isEmpty(h.getObjectId())) ids.add(h.getObjectId());
        return ids;
    }

    /** Assigned hook ids belonging to the given groups (by known hook group). */
    public static List<String> assignedIdsInGroups(Snapshot s, AppXpPacket app, Collection<String> groups) {
        List<String> ids = new ArrayList<>();
        Set<String> lower = new HashSet<>();
        for (String g : groups) if (g != null) lower.add(g.toLowerCase(Locale.ROOT));
        synchronized (app.assignments) {
            for (AssignmentPacket a : app.assignments) {
                String g = groupOfAssignment(s, a);
                String id = a == null ? null : a.getHookId();
                if (g != null && id != null && lower.contains(g.toLowerCase(Locale.ROOT))) ids.add(id);
            }
        }
        return ids;
    }

    private static void push(final Context context, final AppXpPacket app, final List<String> ids, final boolean delete, final Done done) {
        mutations++;
        recordChange(context, app);
        final Context ctx = context.getApplicationContext();
        final int uid = app.uid;
        final String pkg = app.packageName;
        final boolean kill = app.forceStop;
        EXECUTOR.submit(() -> {
            A_CODE code;
            try {
                code = AssignHooksCommand.call(ctx, AssignmentsPacket.create(uid, pkg, ids, delete, kill));
            } catch (Throwable t) {
                Log.e(TAG, "Assign failed pkg=" + pkg + " delete=" + delete + " " + t);
                code = A_CODE.FAILED;
            }
            Log.i(TAG, "Assign pkg=" + pkg + " count=" + ids.size() + " delete=" + delete + " result=" + code);
            if (done != null) done.onDone(code);
        });
    }

    // ---- recently changed apps (local only, for the Home dashboard) ----------

    private static final String PREF_RECENT = "identra_recent_apps";
    private static final int MAX_RECENT = 8;

    public static final class RecentChange {
        public String packageName, label;
        public int uid;
        public long time;
        public boolean protectedNow;
    }

    static void recordChange(Context context, AppXpPacket app) {
        try {
            android.content.SharedPreferences prefs = IdentraTheme.prefs(context);
            org.json.JSONArray old = new org.json.JSONArray(prefs.getString(PREF_RECENT, "[]"));
            org.json.JSONArray out = new org.json.JSONArray();
            org.json.JSONObject now = new org.json.JSONObject();
            now.put("p", app.packageName);
            now.put("u", app.uid);
            now.put("l", app.label == null ? app.packageName : app.label);
            now.put("t", System.currentTimeMillis());
            now.put("on", isProtected(app));
            out.put(now);
            for (int i = 0; i < old.length() && out.length() < MAX_RECENT; i++) {
                org.json.JSONObject o = old.optJSONObject(i);
                if (o == null) continue;
                if (app.packageName.equals(o.optString("p")) && app.uid == o.optInt("u")) continue;
                out.put(o);
            }
            prefs.edit().putString(PREF_RECENT, out.toString()).apply();
        } catch (Throwable t) {
            Log.w(TAG, "Recent change not recorded: " + t);
        }
    }

    public static List<RecentChange> recentChanges(Context context) {
        List<RecentChange> list = new ArrayList<>();
        try {
            org.json.JSONArray arr = new org.json.JSONArray(IdentraTheme.prefs(context).getString(PREF_RECENT, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                RecentChange r = new RecentChange();
                r.packageName = o.optString("p");
                r.uid = o.optInt("u");
                r.label = o.optString("l", r.packageName);
                r.time = o.optLong("t");
                r.protectedNow = o.optBoolean("on");
                if (!Str.isEmpty(r.packageName)) list.add(r);
            }
        } catch (Throwable ignored) { }
        return list;
    }

    // ---- loading helpers -------------------------------------------------------

    public interface Loaded { void onLoaded(Snapshot snapshot); }

    /** Uses the cached snapshot when there is one, otherwise loads in the background. Callback on main thread. */
    public static void ensure(Context context, final Loaded callback) {
        Snapshot c = cache;
        if (c != null) { callback.onLoaded(c); return; }
        refresh(context, callback);
    }

    /** Always reloads in the background. Callback on main thread (snapshot may be null on failure). */
    public static void refresh(Context context, final Loaded callback) {
        final Context ctx = context.getApplicationContext();
        final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        EXECUTOR.submit(() -> {
            Snapshot s = null;
            try { s = load(ctx); } catch (Throwable t) { Log.e(TAG, "Load failed: " + t); }
            final Snapshot fs = cache; // load() keeps the newest non-stale snapshot in cache
            h.post(() -> callback.onLoaded(fs));
        });
    }

    public static List<AppXpPacket> protectedApps(Snapshot s) {
        List<AppXpPacket> out = new ArrayList<>();
        if (s == null) return out;
        for (AppXpPacket a : s.apps) if (isProtected(a)) out.add(a);
        return out;
    }

    /** 0.7.23: clone user id -> "Instance N" number, in the order the Instances screen shows them. */
    /** Instance user -> the user-picked apps in it (Play Services excluded). */
    public static Map<Integer, List<String>> instanceApps(Context context) {
        Map<Integer, List<String>> out = new LinkedHashMap<>();
        try {
            String raw = android.provider.Settings.Global.getString(context.getContentResolver(), "identra_clone_allow");
            Map<Integer, List<String>> cfg = dev.identra.app.ui.tools.IdentraInstancesActivity.parse(raw == null ? "" : raw);
            for (Map.Entry<Integer, List<String>> e : cfg.entrySet()) {
                List<String> a = dev.identra.app.ui.tools.IdentraInstancesActivity.userApps(e.getValue());
                if (!a.isEmpty()) out.put(e.getKey(), a);
            }
        } catch (Throwable t) { Log.w(TAG, "instances: " + t); }
        return out;
    }

    public static Map<Integer, String> instanceNumbers(Context context) {
        Map<Integer, String> out = new java.util.HashMap<>();
        try {
            String raw = android.provider.Settings.Global.getString(context.getContentResolver(), "identra_clone_allow");
            Map<Integer, List<String>> cfg = dev.identra.app.ui.tools.IdentraInstancesActivity.parse(raw == null ? "" : raw);
            for (Map.Entry<Integer, List<String>> e : cfg.entrySet())
                if (!dev.identra.app.ui.tools.IdentraInstancesActivity.userApps(e.getValue()).isEmpty())
                    out.put(e.getKey(), dev.identra.app.ui.tools.InstanceNames.get(context, e.getKey()));
        } catch (Throwable t) {
            Log.w(TAG, "instances: " + t);
        }
        return out;
    }

    /** Apps running in another user get " · Instance N" so they can't be mistaken for the main-phone copy. */
    private static void markInstance(AppXpPacket a, Map<Integer, String> inst) {
        int user = a.uid / 100000;
        if (user == 0) return;
        String base = a.label == null || a.label.isEmpty() ? a.packageName : a.label;
        if (base.contains(" · ")) return;
        String n = inst.get(user);
        a.label = base + " · " + (n != null ? n : "User " + user);
    }

    public static boolean isInstance(AppXpPacket app) { return app != null && app.uid / 100000 != 0; }

    public static String labelOf(AppXpPacket app) {
        if (app == null) return "";
        return app.label == null || app.label.isEmpty() ? app.packageName : app.label;
    }

    // ---- display helpers ------------------------------------------------------

    public static String groupTitle(Context context, String name) {
        if (name == null) return "";
        Resources res = context.getResources();
        String g = name.toLowerCase().replaceAll("[^a-z]", "_");
        int id = res.getIdentifier("group_" + g, "string", context.getPackageName());
        return id > 0 ? res.getString(id) : name;
    }
}
