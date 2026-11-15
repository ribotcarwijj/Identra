package dev.identra.app.found;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import dev.identra.app.x.core.commands.call.GetSettingExCommand;
import dev.identra.app.x.core.settings.data.SettingPacket;

/**
 * App-side state of "Found in this app" (0.6.0):
 * <ul>
 *   <li>static scan results ("Likely"), cached per package + versionCode + lastUpdateTime;</li>
 *   <li>observed hits ("Seen"), read from the per-app setting system_server keeps;</li>
 *   <li>which assignments the "Use identity for everything found" switch made (so turning it off
 *       removes exactly those).</li>
 * </ul>
 */
public final class FoundStore {
    private static final String TAG = "Identra.Found";
    private static final String PREFS_SCAN = "identra_found_scan";
    private static final String PREFS_STATE = "identra_found_state";

    private FoundStore() { }

    public static final class Found {
        /** id -> true if seen firing, false if only likely from the scan. Insertion ordered. */
        public final Map<String, Boolean> ids = new LinkedHashMap<>();
        public boolean scanned;
        public long scanMillis;
        public int seenCount() { int n = 0; for (Boolean b : ids.values()) if (b) n++; return n; }
    }

    // ---- scan cache ----

    static String stampOf(Context context, String pkg) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
            long vc = Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
            return vc + ":" + pi.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /** Cached scan ids if still valid for the installed version, else null. */
    public static Set<String> cachedScan(Context context, String pkg) {
        SharedPreferences p = context.getSharedPreferences(PREFS_SCAN, Context.MODE_PRIVATE);
        String stamp = stampOf(context, pkg);
        if (stamp == null || !stamp.equals(p.getString(pkg + "|stamp", null))) return null;
        return FoundIndex.parseIds(p.getString(pkg + "|ids", ""));
    }

    /** Scans now (blocking) unless a valid cache exists or force is set. Returns the likely ids. */
    public static Set<String> scan(Context context, String pkg, boolean force) {
        if (!force) {
            Set<String> c = cachedScan(context, pkg);
            if (c != null) return c;
        }
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(pkg, 0);
            DexScanner.Result r = DexScanner.scan(ai.sourceDir, ai.splitSourceDirs);
            String stamp = stampOf(context, pkg);
            if (r.dexFiles > 0 && stamp != null) {
                context.getSharedPreferences(PREFS_SCAN, Context.MODE_PRIVATE).edit()
                        .putString(pkg + "|stamp", stamp)
                        .putString(pkg + "|ids", FoundIndex.joinIds(r.ids))
                        .putLong(pkg + "|ms", r.millis)
                        .apply();
            }
            Log.i(TAG, "scan " + pkg + " dex=" + r.dexFiles + " refs=" + r.methodRefs + " strings=" + r.strings
                    + " found=" + r.ids.size() + " in " + r.millis + " ms" + (r.error != null ? " error=" + r.error : ""));
            return r.ids;
        } catch (Throwable t) {
            Log.w(TAG, "scan " + pkg + " failed: " + t);
            return new HashSet<>();
        }
    }

    public static void forgetScan(Context context, String pkg) {
        context.getSharedPreferences(PREFS_SCAN, Context.MODE_PRIVATE).edit()
                .remove(pkg + "|stamp").remove(pkg + "|ids").remove(pkg + "|ms").apply();
    }

    // ---- seen (observe mode) ----

    /** Seen ids: the app's own file (read with root, works with any system part) + the system setting if present. */
    public static Set<String> seen(Context context, int uid, String pkg) {
        Set<String> out = new HashSet<>();
        try {
            SettingPacket p = GetSettingExCommand.get(context, FoundIndex.SETTING_SEEN, uid, pkg);
            out.addAll(FoundIndex.parseIds(p == null ? null : p.value));
        } catch (Throwable ignored) { }
        out.addAll(seenLocal(context, pkg));
        return out;
    }

    /** Reads <dataDir>/no_backup/.identra_found of the app with root. */
    static Set<String> seenLocal(Context context, String pkg) {
        Set<String> out = new HashSet<>();
        try {
            if (!dev.identra.app.ui.profiles.AppDataVault.hasRoot(false)) return out;
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(pkg, 0);
            if (ai.dataDir == null || !dev.identra.app.ui.profiles.AppDataVault.safeName(pkg)) return out;
            String path = ai.dataDir + "/no_backup/" + FoundObserver.LOCAL_FILE;
            dev.identra.app.ui.profiles.AppDataVault.Result r =
                    dev.identra.app.ui.profiles.AppDataVault.run("cat '" + path + "' 2>/dev/null", 10);
            if (r != null && r.output != null)
                for (String line : r.output.split("\n")) {
                    line = line.trim();
                    if (line.startsWith("Identra.")) out.add(line);
                }
        } catch (Throwable t) {
            Log.w(TAG, "seen file " + pkg + ": " + t);
        }
        return out;
    }

    public static Set<String> autoAdded(Context context, int uid, String pkg) {
        try {
            SettingPacket p = GetSettingExCommand.get(context, FoundIndex.SETTING_AUTO_ADDED, uid, pkg);
            return FoundIndex.parseIds(p == null ? null : p.value);
        } catch (Throwable t) {
            return new HashSet<>();
        }
    }

    /** Seen + likely, only detectable hooks of the index. Blocking (scan may run). */
    public static Found load(Context context, int uid, String pkg, boolean forceScan) {
        Found f = new Found();
        FoundIndex index = FoundIndex.get();
        Set<String> seen = seen(context, uid, pkg);
        Set<String> likely = scan(context, pkg, forceScan);
        f.scanned = true;
        for (FoundIndex.Entry e : index.entries) {
            if (seen.contains(e.id)) f.ids.put(e.id, true);
        }
        for (FoundIndex.Entry e : index.entries) {
            if (!f.ids.containsKey(e.id) && likely.contains(e.id)) f.ids.put(e.id, false);
        }
        return f;
    }

    // ---- switch state ----

    private static String key(String pkg, int uid) { return pkg + ":" + uid; }

    public static boolean isIdentityOn(Context context, String pkg, int uid) {
        return context.getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE).getBoolean(key(pkg, uid) + "|on", false);
    }

    public static Set<String> managed(Context context, String pkg, int uid) {
        return FoundIndex.parseIds(context.getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE)
                .getString(key(pkg, uid) + "|ids", ""));
    }

    static void setState(Context context, String pkg, int uid, boolean on, Set<String> managedIds) {
        context.getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE).edit()
                .putBoolean(key(pkg, uid) + "|on", on)
                .putString(key(pkg, uid) + "|ids", on ? FoundIndex.joinIds(managedIds) : "")
                .apply();
    }

    /** Packages (pkg:uid) with the switch on. */
    public static Set<String> identityOnKeys(Context context) {
        Set<String> out = new HashSet<>();
        for (Map.Entry<String, ?> e : context.getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE).getAll().entrySet())
            if (e.getKey().endsWith("|on") && Boolean.TRUE.equals(e.getValue()))
                out.add(e.getKey().substring(0, e.getKey().length() - 3));
        return out;
    }
}
