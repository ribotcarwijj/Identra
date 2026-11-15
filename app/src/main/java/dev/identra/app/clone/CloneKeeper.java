package dev.identra.app.clone;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.identra.xposed.XC_MethodHook;
import dev.identra.xposed.XposedBridge;

/**
 * 0.7.16 Clone keeper (system_server).
 *
 * Config: Settings.Global "identra_clone_allow" = "10=com.android.chrome|com.google.android.gms;11=pkg"
 * Every user id listed is an Identra clone user. Inside a clone user:
 *  - only the listed packages (plus core system uids) may start a process;
 *    any other process start is refused (ProcessList.newProcessRecordLocked -> null),
 *  - on config change, already running non-listed packages in the clone are force-stopped.
 * Outside the clone users (the owner's view), clone users are removed from the
 * profile / user lists UserManagerService hands to other processes, so Settings,
 * the launcher, SystemUI and apps see only the owner.
 *
 * The hooks never read Settings themselves (AMS lock): a ContentObserver fills a volatile cache.
 */
public final class CloneKeeper {
    private static final String TAG = "Identra.Clone";
    public static final String KEY = "identra_clone_allow";

    private static volatile Map<Integer, Set<String>> clones = Collections.emptyMap();
    private static volatile Set<Integer> cloneIds = Collections.emptySet();
    private static ContentObserver observer;
    private static HandlerThread thread;
    private static Context ctx;
    private static boolean hooked = false;
    private static int identraAppId = -1;

    private static final Set<String> CORE = new HashSet<>();
    static {
        CORE.add("android");
        CORE.add("com.android.keychain");
        CORE.add("com.android.certinstaller");
        CORE.add("com.android.permissioncontroller");
        CORE.add("com.google.android.permissioncontroller");
        CORE.add("com.android.providers.settings");
        CORE.add("com.android.providers.media.module");
        CORE.add("com.google.android.providers.media.module");
        CORE.add("com.android.externalstorage");
        CORE.add("com.google.android.webview");
        CORE.add("com.android.webview");
        CORE.add("dev.identra.app");
    }

    private CloneKeeper() { }

    public static synchronized void install(Context context, ClassLoader loader) {
        ctx = context;
        try {
            identraAppId = context.getPackageManager().getPackageUid("dev.identra.app", 0) % 100000;
        } catch (Throwable ignored) { }
        if (!hooked) {
            hookProcessStart(loader);
            hookUserLists(loader);
            hookCloneLimit(loader);
            hookLauncherApps(loader);
            hooked = true;
        }
        startObserver();
    }

    public static synchronized void shutdown() {
        try { if (observer != null && ctx != null) ctx.getContentResolver().unregisterContentObserver(observer); } catch (Throwable ignored) { }
        observer = null;
        try { if (thread != null) thread.quitSafely(); } catch (Throwable ignored) { }
        thread = null;
        hooked = false; // old hooks are dropped by the framework on hot reload
    }

    // ---------- config ----------

    private static void startObserver() {
        if (observer != null) return;
        thread = new HandlerThread("identra-clone");
        thread.start();
        final Handler h = new Handler(thread.getLooper());
        observer = new ContentObserver(h) {
            @Override public void onChange(boolean selfChange) { reload(true); }
        };
        try {
            ctx.getContentResolver().registerContentObserver(Settings.Global.getUriFor(KEY), false, observer);
        } catch (Throwable t) {
            Log.w(TAG, "observer: " + t);
        }
        h.post(new Runnable() { @Override public void run() { reload(true); } });
    }

    static void reload(boolean sweep) {
        String raw = null;
        try { raw = Settings.Global.getString(ctx.getContentResolver(), KEY); } catch (Throwable t) { Log.w(TAG, "read: " + t); }
        Map<Integer, Set<String>> m = parse(raw);
        clones = m;
        cloneIds = Collections.unmodifiableSet(new HashSet<>(m.keySet()));
        Log.i(TAG, "config " + m);
        if (sweep) sweep();
    }

    static Map<Integer, Set<String>> parse(String raw) {
        Map<Integer, Set<String>> m = new HashMap<>();
        if (raw == null) return m;
        for (String part : raw.split(";")) {
            part = part.trim();
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            try {
                int user = Integer.parseInt(part.substring(0, eq).trim());
                if (user <= 0) continue; // never the owner
                Set<String> pkgs = new HashSet<>();
                for (String p : part.substring(eq + 1).split("[|,]")) {
                    p = p.trim();
                    if (!p.isEmpty()) pkgs.add(p);
                }
                m.put(user, Collections.unmodifiableSet(pkgs));
            } catch (NumberFormatException ignored) { }
        }
        return Collections.unmodifiableMap(m);
    }

    static boolean allowed(int userId, String pkg, int uid) {
        Set<String> allow = clones.get(userId);
        if (allow == null) return true;               // not a clone user
        int appId = uid % 100000;
        if (appId < Process.FIRST_APPLICATION_UID) return true;   // core system uids
        if (pkg == null) return true;
        if (CORE.contains(pkg)) return true;
        if (pkg.startsWith("com.google.android.trichromelibrary")) return true;
        return allow.contains(pkg);
    }

    // ---------- process start ----------

    private static void hookProcessStart(ClassLoader loader) {
        try {
            Class<?> pl = Class.forName("com.android.server.am.ProcessList", false, loader);
            Set<?> s = XposedBridge.hookAllMethods(pl, "newProcessRecordLocked", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if (clones.isEmpty()) return;
                        ApplicationInfo ai = null;
                        for (Object a : param.args) if (a instanceof ApplicationInfo) { ai = (ApplicationInfo) a; break; }
                        if (ai == null) return;
                        int userId = ai.uid / 100000;
                        if (!allowed(userId, ai.packageName, ai.uid)) {
                            Log.i(TAG, "blocked start " + ai.packageName + " u" + userId);
                            param.setResult(null);
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "start hook: " + t);
                    }
                }
            });
            Log.i(TAG, "process hook x" + s.size());
        } catch (Throwable t) {
            Log.e(TAG, "process hook failed: " + t);
        }
    }

    /** Force-stop running packages in clone users that are not on their list. */
    private static void sweep() {
        if (clones.isEmpty() || ctx == null) return;
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
            if (procs == null) return;
            Set<String> done = new HashSet<>();
            java.lang.reflect.Method stop = ActivityManager.class.getMethod("forceStopPackageAsUser", String.class, int.class);
            for (ActivityManager.RunningAppProcessInfo p : procs) {
                int userId = p.uid / 100000;
                if (!clones.containsKey(userId) || p.pkgList == null) continue;
                for (String pkg : p.pkgList) {
                    if (allowed(userId, pkg, p.uid)) continue;
                    if (!done.add(userId + ":" + pkg)) continue;
                    long id = Binder.clearCallingIdentity();
                    try {
                        stop.invoke(am, pkg, userId);
                        Log.i(TAG, "swept " + pkg + " u" + userId);
                    } catch (Throwable t) {
                        Log.w(TAG, "sweep " + pkg + ": " + t);
                    } finally {
                        Binder.restoreCallingIdentity(id);
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "sweep: " + t);
        }
    }

    // ---------- hide from the owner's view ----------

    private static boolean shouldHide(Object[] args) {
        if (cloneIds.isEmpty()) return false;
        if (Binder.getCallingPid() == Process.myPid()) return false;   // system_server itself
        int uid = Binder.getCallingUid();
        int appId = uid % 100000;
        int callerUser = uid / 100000;
        if (appId == Process.SHELL_UID || appId == Process.ROOT_UID) return false;
        if (identraAppId > 0 && appId == identraAppId) return false;
        if (cloneIds.contains(callerUser)) return false;               // processes inside a clone
        // a query about a clone user itself is answered normally
        if (args != null && args.length > 0 && args[0] instanceof Integer && cloneIds.contains((Integer) args[0]))
            return false;
        return true;
    }

    private static void hookUserLists(ClassLoader loader) {
        try {
            Class<?> ums = Class.forName("com.android.server.pm.UserManagerService", false, loader);
            XC_MethodHook filter = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object r = param.getResult();
                        if (r == null || !shouldHide(param.args)) return;
                        Set<Integer> hide = cloneIds;
                        if (r instanceof int[]) {
                            int[] in = (int[]) r;
                            int n = 0;
                            for (int v : in) if (!hide.contains(v)) n++;
                            if (n == in.length) return;
                            int[] out = new int[n];
                            int i = 0;
                            for (int v : in) if (!hide.contains(v)) out[i++] = v;
                            param.setResult(out);
                        } else if (r instanceof List) {
                            List<?> in = (List<?>) r;
                            List<Object> out = new ArrayList<>(in.size());
                            boolean changed = false;
                            for (Object o : in) {
                                int id = userId(o);
                                if (id >= 0 && hide.contains(id)) { changed = true; continue; }
                                out.add(o);
                            }
                            if (changed) param.setResult(out);
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "hide: " + t);
                    }
                }
            };
            int n = 0;
            for (String m : new String[]{"getProfiles", "getProfileIds", "getUsers", "getUserProfiles",
                    "getEnabledProfileIds", "getProfilesAsUser"}) {
                try { n += XposedBridge.hookAllMethods(ums, m, filter).size(); } catch (Throwable ignored) { }
            }
            Log.i(TAG, "user list hooks x" + n);
        } catch (Throwable t) {
            Log.e(TAG, "user list hook failed: " + t);
        }
    }

    /** Allow several CLONE profiles under the owner (stock: 1 per parent). */
    public static final int MAX_CLONES = 8;

    private static void hookCloneLimit(ClassLoader loader) {
        try {
            Class<?> utd = Class.forName("com.android.server.pm.UserTypeDetails", false, loader);
            XC_MethodHook raise = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object name = param.thisObject.getClass().getMethod("getName").invoke(param.thisObject);
                        if (!"android.os.usertype.profile.CLONE".equals(name)) return;
                        Object r = param.getResult();
                        if (r instanceof Integer && (Integer) r >= 0 && (Integer) r < MAX_CLONES) param.setResult(MAX_CLONES);
                    } catch (Throwable ignored) { }
                }
            };
            int n = XposedBridge.hookAllMethods(utd, "getMaxAllowed", raise).size()
                    + XposedBridge.hookAllMethods(utd, "getMaxAllowedPerParent", raise).size();
            Log.i(TAG, "clone limit hooks x" + n);
        } catch (Throwable t) {
            Log.e(TAG, "clone limit hook failed: " + t);
        }
    }

    // ---------- 0.7.19: keep clone apps off the home screen (LauncherApps) ----------

    private static int handleUser(Object[] args) {
        if (args == null) return -1;
        for (Object a : args) {
            if (a != null && "android.os.UserHandle".equals(a.getClass().getName())) {
                int id = userId(a);
                if (id >= 0) return id;
            }
        }
        return -1;
    }

    private static Object emptyFor(Class<?> rt) {
        try {
            if (rt == boolean.class) return false;
            if (rt == int.class) return 0;
            if (rt == long.class) return 0L;
            if ("android.content.pm.ParceledListSlice".equals(rt.getName()))
                return rt.getConstructor(List.class).newInstance(new ArrayList<>());
            if (List.class.isAssignableFrom(rt)) return new ArrayList<>();
        } catch (Throwable ignored) { }
        return null;
    }

    private static void hookLauncherApps(ClassLoader loader) {
        try {
            Class<?> impl = Class.forName("com.android.server.pm.LauncherAppsService$LauncherAppsImpl", false, loader);
            XC_MethodHook block = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        int u = handleUser(param.args);
                        if (u < 0 || !cloneIds.contains(u) || !shouldHide(null)) return;
                        Class<?> rt = ((java.lang.reflect.Method) param.method).getReturnType();
                        if (rt == void.class) return;
                        param.setResult(emptyFor(rt));
                    } catch (Throwable t) {
                        Log.w(TAG, "launcher block: " + t);
                    }
                }
            };
            int n = 0;
            for (String m : new String[]{"getLauncherActivities", "resolveLauncherActivityInternal", "resolveLauncherActivity",
                    "isPackageEnabled", "isActivityEnabled", "getApplicationInfo", "getSuspendedPackageLauncherExtras",
                    "getShortcutConfigActivities", "getAllSessions", "hasShortcutHostPermission", "getLauncherUserInfo",
                    "getPreInstalledSystemPackages", "getAppMarketActivityIntent", "getPrivateSpaceSettingsIntent"}) {
                try { n += XposedBridge.hookAllMethods(impl, m, block).size(); } catch (Throwable ignored) { }
            }
            XC_MethodHook filterProfiles = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object r = param.getResult();
                        if (!(r instanceof List) || !shouldHide(null)) return;
                        List<Object> out = new ArrayList<>();
                        boolean changed = false;
                        for (Object o : (List<?>) r) {
                            int id = userId(o);
                            if (id >= 0 && cloneIds.contains(id)) { changed = true; continue; }
                            out.add(o);
                        }
                        if (changed) param.setResult(out);
                    } catch (Throwable t) {
                        Log.w(TAG, "launcher profiles: " + t);
                    }
                }
            };
            try { n += XposedBridge.hookAllMethods(impl, "getUserProfiles", filterProfiles).size(); } catch (Throwable ignored) { }
            Log.i(TAG, "launcher hooks x" + n);
        } catch (Throwable t) {
            Log.e(TAG, "launcher hook failed: " + t);
        }
    }

    private static int userId(Object o) {
        if (o == null) return -1;
        try {
            return o.getClass().getField("id").getInt(o);          // UserInfo.id
        } catch (Throwable ignored) { }
        try {
            return (Integer) o.getClass().getMethod("getIdentifier").invoke(o); // UserHandle
        } catch (Throwable ignored) { }
        return -1;
    }
}
