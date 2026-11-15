package dev.identra.app.found;

import android.content.Context;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import dev.identra.app.hooks.XHookUtil;
import dev.identra.app.x.core.commands.call.FoundReportCommand;
import dev.identra.xposed.XposedBridge;
import io.github.libxposed.api.XposedInterface;

/**
 * 0.6.0 observe mode: runs inside every hooked app process and notes which Identra hooks the app
 * really triggers ("Seen"), without changing any value (observe only = note the hit, return the real
 * result).
 * <p>
 * Cost model, so it can stay on for every scoped app:
 * <ul>
 *   <li>method observers: one tiny interceptor per target method that is not assigned anyway; it
 *       notes its hook ids on the first call and removes itself, so a method costs nothing after
 *       that (and nothing at all if the app never calls it);</li>
 *   <li>key observers (SystemProperties.get*, Settings.*.getStringForUser, ProcessBuilder.start):
 *       stay installed; per call one map lookup of the key, cached;</li>
 *   <li>installing: method observers are installed on the calling thread for at most
 *       {@link #SYNC_BUDGET_MS}, the rest continue on a background thread;</li>
 *   <li>reporting: new ids are batched, deduplicated per process and sent to system_server at most
 *       every {@link #MIN_FLUSH_GAP_MS} ms, at most {@link #MAX_FLUSHES} times per process.</li>
 * </ul>
 * Field hooks (Build.MODEL ...) are read without a method call and can only be found by the scan.
 */
public final class FoundObserver {
    private static final String TAG = "Identra.Observe";
    private static final long SYNC_BUDGET_MS = 25;
    private static final long FIRST_FLUSH_DELAY_MS = 4000;
    private static final long MIN_FLUSH_GAP_MS = 15000;
    private static final int MAX_FLUSHES = 40;

    private static final Set<String> seen = ConcurrentHashMap.newKeySet();
    private static final Set<String> pending = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean flushScheduled = new AtomicBoolean(false);
    private static final AtomicInteger flushes = new AtomicInteger(0);
    private static volatile long lastFlush = 0;
    private static volatile Context appContext;
    private static volatile boolean active = false;
    private static ScheduledExecutorService worker;

    private static final String[] NONE = new String[0];
    private static final Map<String, String[]> propCache = new ConcurrentHashMap<>();
    private static final Map<String, String[]> settingCache = new ConcurrentHashMap<>();

    private FoundObserver() { }

    /** Records that an (assigned) Identra hook ran in this process. Cheap; safe to call from any hook. */
    public static void hit(String hookId) {
        if (!active || hookId == null || seen.contains(hookId)) return;
        if (!FoundIndex.get().byId.containsKey(hookId)) return;
        if (seen.add(hookId)) {
            pending.add(hookId);
            scheduleFlush();
        }
    }

    private static void hitAll(String[] ids) {
        for (String id : ids) {
            if (seen.add(id)) {
                pending.add(id);
                scheduleFlush();
            }
        }
    }

    /**
     * Installs the observers for this app process. Call once, after the app's own Identra hooks.
     * @param assigned hook ids assigned to the app (they report through {@link #hit} instead)
     * @param settings the app's resolved settings (global switch {@link FoundIndex#SETTING_OBSERVE})
     */
    public static synchronized void install(Context context, ClassLoader loader, Collection<String> assigned, Map<String, String> settings) {
        if (active) return;
        try {
            int appId = Process.myUid() % 100000;
            if (appId < 10000 || appId > 19999) return;                    // apps only, never system processes
            if (settings != null && "false".equalsIgnoreCase(settings.get(FoundIndex.SETTING_OBSERVE))) return;
            XposedInterface xi = XposedBridge.framework();
            if (xi == null || context == null) return;
            appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
            worker = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "identra-observe");
                t.setDaemon(true);
                return t;
            });
            active = true;
            long t0 = SystemClock.elapsedRealtime();
            FoundIndex index = FoundIndex.get();
            installKeyObservers(xi, loader, index);
            final List<Runnable> methodJobs = methodObservers(xi, context, loader, index, assigned == null ? new HashSet<>() : new HashSet<>(assigned));
            int sync = 0;
            while (sync < methodJobs.size() && SystemClock.elapsedRealtime() - t0 < SYNC_BUDGET_MS) methodJobs.get(sync++).run();
            final int start = sync;
            if (start < methodJobs.size()) worker.execute(() -> { for (int i = start; i < methodJobs.size(); i++) methodJobs.get(i).run(); });
            Log.i(TAG, context.getPackageName() + " observers: methods=" + methodJobs.size() + " sync=" + sync
                    + " in " + (SystemClock.elapsedRealtime() - t0) + " ms");
        } catch (Throwable t) {
            Log.w(TAG, "install failed: " + t);
        }
    }

    // ---- key observers (stay installed) ----

    private static void installKeyObservers(XposedInterface xi, ClassLoader loader, final FoundIndex index) {
        if (!index.props.isEmpty()) {
            try {
                Class<?> sp = Class.forName("android.os.SystemProperties", false, loader);
                for (Method m : sp.getDeclaredMethods()) {
                    String n = m.getName();
                    if (!(n.equals("get") || n.equals("getInt") || n.equals("getLong") || n.equals("getBoolean"))) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 0 || p[0] != String.class || Modifier.isAbstract(m.getModifiers())) continue;
                    xi.hook(m).intercept(chain -> {
                        Object k = chain.getArg(0);
                        if (k instanceof String) onProp((String) k);
                        return chain.proceed();
                    });
                }
            } catch (Throwable t) {
                Log.w(TAG, "props observer: " + t);
            }
        }
        if (!index.settingsKeys.isEmpty()) {
            for (String cls : new String[]{"android.provider.Settings$Secure", "android.provider.Settings$Global", "android.provider.Settings$System"}) {
                try {
                    Class<?> c = Class.forName(cls, false, loader);
                    boolean done = false;
                    for (Method m : c.getDeclaredMethods()) {
                        Class<?>[] p = m.getParameterTypes();
                        if (!m.getName().equals("getStringForUser") || p.length != 3 || p[1] != String.class) continue;
                        xi.hook(m).intercept(chain -> {
                            Object k = chain.getArg(1);
                            if (k instanceof String) onSetting((String) k);
                            return chain.proceed();
                        });
                        done = true;
                    }
                    if (!done) {
                        for (Method m : c.getDeclaredMethods()) {
                            Class<?>[] p = m.getParameterTypes();
                            if (!m.getName().equals("getString") || p.length != 2 || p[1] != String.class) continue;
                            xi.hook(m).intercept(chain -> {
                                Object k = chain.getArg(1);
                                if (k instanceof String) onSetting((String) k);
                                return chain.proceed();
                            });
                        }
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "settings observer " + cls + ": " + t);
                }
            }
        }
        if (!index.shellWords.isEmpty() || !index.props.isEmpty()) {
            try {
                Method start = ProcessBuilder.class.getDeclaredMethod("start");
                xi.hook(start).intercept(chain -> {
                    try {
                        Object pb = chain.getThisObject();
                        if (pb instanceof ProcessBuilder) onCommand(((ProcessBuilder) pb).command());
                    } catch (Throwable ignored) { }
                    return chain.proceed();
                });
            } catch (Throwable t) {
                Log.w(TAG, "shell observer: " + t);
            }
        }
    }

    private static void onProp(String key) {
        String[] ids = propCache.get(key);
        if (ids == null) {
            Set<String> out = new LinkedHashSet<>();
            FoundIndex.get().props.match(key, out);
            ids = out.isEmpty() ? NONE : out.toArray(new String[0]);
            if (propCache.size() < 4096) propCache.put(key, ids);
        }
        if (ids.length > 0) hitAll(ids);
    }

    private static void onSetting(String key) {
        String[] ids = settingCache.get(key);
        if (ids == null) {
            Set<String> out = new LinkedHashSet<>();
            FoundIndex.get().settingsKeys.match(key, out);
            ids = out.isEmpty() ? NONE : out.toArray(new String[0]);
            if (settingCache.size() < 4096) settingCache.put(key, ids);
        }
        if (ids.length > 0) hitAll(ids);
    }

    private static void onCommand(List<String> cmd) {
        if (cmd == null || cmd.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (String s : cmd) { if (sb.length() > 0) sb.append(' '); sb.append(s); }
        String line = sb.toString();
        String low = line.toLowerCase(java.util.Locale.ROOT);
        int gp = low.indexOf("getprop");
        if (gp >= 0) {
            String[] parts = line.substring(gp).trim().split("\\s+");
            if (parts.length > 1) onProp(parts[1]);
        }
        for (Map.Entry<String, List<String>> e : FoundIndex.get().shellWords.entrySet()) {
            String w = e.getKey();
            if (low.startsWith(w + " ") || low.equals(w) || low.contains("/" + w) || low.contains(" " + w + " ") || low.endsWith(" " + w))
                hitAll(e.getValue().toArray(new String[0]));
        }
    }

    // ---- method observers (remove themselves after the first call) ----

    private static List<Runnable> methodObservers(final XposedInterface xi, Context context, ClassLoader loader,
                                                  FoundIndex index, Set<String> assigned) {
        // executable -> ids, built lazily per class inside the jobs; framework classes first
        List<String> classes = new ArrayList<>(index.members.keySet());
        java.util.Collections.sort(classes, (a, b) -> Integer.compare(rank(a), rank(b)));
        List<Runnable> jobs = new ArrayList<>(classes.size());
        for (final String cls : classes) {
            final Map<String, List<String>> byName = index.members.get(cls);
            jobs.add(() -> observeClass(xi, context, loader, cls, byName, assigned));
        }
        return jobs;
    }

    private static int rank(String cls) {
        if (cls.startsWith("android.") || cls.startsWith("java.")) return 0;
        if (cls.startsWith("libcore.") || cls.startsWith("com.android.")) return 1;
        return 2;
    }

    private static void observeClass(XposedInterface xi, Context context, ClassLoader loader, String cls,
                                     Map<String, List<String>> byName, Set<String> assigned) {
        try {
            String resolved = cls;
            try { resolved = XHookUtil.resolveClassName(context, cls); } catch (Throwable ignored) { }
            if (resolved == null) resolved = cls;
            Class<?> c;
            try { c = Class.forName(resolved, false, loader); }
            catch (Throwable t) { return; }                                   // SDK not in this app: nothing to observe
            Map<Executable, Set<String>> targets = new HashMap<>();
            for (Map.Entry<String, List<String>> e : byName.entrySet()) {
                String name = e.getKey();
                if (name.startsWith("#") || name.equals("*")) continue;      // fields / wildcards: scan only
                List<String> ids = new ArrayList<>();
                for (String id : e.getValue()) if (!assigned.contains(id)) ids.add(id);
                if (ids.isEmpty()) continue;
                for (Method m : methodsNamed(c, name)) {
                    Set<String> s = targets.get(m);
                    if (s == null) { s = new LinkedHashSet<>(); targets.put(m, s); }
                    s.addAll(ids);
                }
            }
            for (Map.Entry<Executable, Set<String>> t : targets.entrySet()) once(xi, t.getKey(), t.getValue().toArray(new String[0]));
        } catch (Throwable t) {
            Log.w(TAG, "observe " + cls + ": " + t);
        }
    }

    /** Non-abstract methods with this name, from the class or the nearest superclass that declares one. */
    private static List<Method> methodsNamed(Class<?> c, String name) {
        List<Method> out = new ArrayList<>();
        for (Class<?> k = c; k != null && k != Object.class && out.isEmpty(); k = k.getSuperclass()) {
            Method[] ms;
            try { ms = k.getDeclaredMethods(); } catch (Throwable t) { break; }
            for (Method m : ms)
                if (m.getName().equals(name) && !Modifier.isAbstract(m.getModifiers()) && !m.isSynthetic() && !m.isBridge()) out.add(m);
        }
        return out;
    }

    private static void once(XposedInterface xi, Executable target, final String[] ids) {
        final AtomicBoolean fired = new AtomicBoolean(false);
        final XposedInterface.HookHandle[] handle = new XposedInterface.HookHandle[1];
        try {
            handle[0] = xi.hook(target).intercept(chain -> {
                if (!fired.get() && fired.compareAndSet(false, true)) {
                    hitAll(ids);
                    final XposedInterface.HookHandle h = handle[0];
                    if (h != null && worker != null) worker.execute(() -> { try { h.unhook(); } catch (Throwable ignored) { } });
                }
                return chain.proceed();
            });
        } catch (Throwable t) {
            // not hookable (native / not allowed): ignore, the scan still covers it
        }
    }

    /** File in the app's own no_backup folder with every hook id seen in it, one per line. */
    public static final String LOCAL_FILE = ".identra_found";

    private static void writeLocal(List<String> batch) {
        try {
            java.io.File dir = appContext.getNoBackupFilesDir();
            java.io.File f = new java.io.File(dir, LOCAL_FILE);
            Set<String> all = new java.util.TreeSet<>();
            if (f.exists()) {
                try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f))) {
                    String line;
                    while ((line = r.readLine()) != null) { line = line.trim(); if (!line.isEmpty()) all.add(line); }
                }
            }
            if (!all.addAll(batch) && f.exists()) return;
            java.io.File tmp = new java.io.File(dir, LOCAL_FILE + ".tmp");
            try (java.io.FileWriter w = new java.io.FileWriter(tmp)) {
                for (String id : all) { w.write(id); w.write('\n'); }
            }
            //noinspection ResultOfMethodCallIgnored
            tmp.renameTo(f);
        } catch (Throwable t) {
            Log.w(TAG, "local write failed: " + t);
        }
    }

    /** Before a hot reload: write what is pending and stop the worker of this generation. */
    public static synchronized void shutdown() {
        if (!active) return;
        active = false;
        try { if (!pending.isEmpty()) flush(); } catch (Throwable ignored) { }
        try { if (worker != null) worker.shutdownNow(); } catch (Throwable ignored) { }
        worker = null;
    }

    // ---- reporting ----

    private static void scheduleFlush() {
        if (worker == null || flushes.get() >= MAX_FLUSHES) return;
        if (!flushScheduled.compareAndSet(false, true)) return;
        long since = SystemClock.elapsedRealtime() - lastFlush;
        long delay = lastFlush == 0 ? FIRST_FLUSH_DELAY_MS : Math.max(0, MIN_FLUSH_GAP_MS - since);
        try {
            worker.schedule(FoundObserver::flush, delay, TimeUnit.MILLISECONDS);
        } catch (Throwable t) {
            flushScheduled.set(false);
        }
    }

    private static void flush() {
        flushScheduled.set(false);
        lastFlush = SystemClock.elapsedRealtime();
        if (pending.isEmpty() || appContext == null) return;
        ArrayList<String> batch = new ArrayList<>(pending);
        pending.removeAll(batch);
        flushes.incrementAndGet();
        // 0.6.1: always kept in the app's own no_backup folder too, so "Seen" works with any system part;
        // the Identra app reads it with root. The system command is used when the system part has it.
        writeLocal(batch);
        boolean ok = FoundReportCommand.report(appContext, batch);
        if (!ok) Log.i(TAG, "seen ids kept locally (system part without the report command) n=" + batch.size());
        if (!pending.isEmpty()) scheduleFlush();
    }
}
