package dev.identra.app.reload;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.service.HookedTarget;
import io.github.libxposed.service.HotReloadResult;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

/**
 * 0.6.1: asks the framework (libxposed service, Vector) to hot-reload Identra in the processes that
 * run it, so an update reaches system_server and running apps without any restart.
 * The framework also does this by itself after an update (module.prop autoHotReload=true); this is
 * the app's own request, used when Home sees that the system part is older than the installed APK.
 * Whether a process accepts is decided by the code already running there (onHotReloading).
 */
public final class HotReloader {
    private static final String TAG = "Identra.Reload";
    private static volatile XposedService service;
    private static boolean registered;

    private HotReloader() { }

    public interface Done {
        /** Called on the main thread. */
        void onDone(Outcome outcome);
    }

    public static final class Outcome {
        public int systemTargets, systemOk, appTargets, appOk;
        public boolean noService;
        public String message;
        public boolean systemReloaded() { return systemTargets > 0 && systemOk == systemTargets; }
    }

    /** Call once from Application.onCreate. */
    public static synchronized void init() {
        if (registered) return;
        registered = true;
        try {
            XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
                @Override public void onServiceBind(XposedService s) {
                    service = s;
                    Log.i(TAG, "framework service " + s.getFrameworkName() + " " + s.getFrameworkVersion() + " api " + s.getApiVersion());
                }
                @Override public void onServiceDied(XposedService s) { if (service == s) service = null; }
            });
        } catch (Throwable t) {
            Log.w(TAG, "service listener: " + t);
        }
    }

    public static boolean hasService() { return service != null; }

    /** The bound framework service, or null when LSPosed has not bound Identra (yet). */
    public static XposedService service() { return service; }

    /** Hot-reloads every process running Identra (system_server first). */
    public static void reloadAll(final Done done) {
        final Handler main = new Handler(Looper.getMainLooper());
        final Outcome out = new Outcome();
        final XposedService s = service;
        if (s == null) {
            out.noService = true;
            main.post(() -> done.onDone(out));
            return;
        }
        new Thread(() -> {
            List<HookedTarget> targets;
            try {
                targets = s.getRunningTargets();
            } catch (Throwable t) {
                out.message = String.valueOf(t);
                main.post(() -> done.onDone(out));
                return;
            }
            List<HookedTarget> list = new ArrayList<>();
            for (HookedTarget t : targets) if (t.getUid() == android.os.Process.SYSTEM_UID) { list.add(t); out.systemTargets++; }
            for (HookedTarget t : targets) if (t.getUid() != android.os.Process.SYSTEM_UID && t.getUid() != android.os.Process.myUid()) { list.add(t); out.appTargets++; }
            if (list.isEmpty()) { main.post(() -> done.onDone(out)); return; }
            final AtomicInteger left = new AtomicInteger(list.size());
            for (final HookedTarget t : list) {
                try {
                    s.hotReloadModule(t, null, (target, result) -> {
                        boolean ok = result.status() == HotReloadResult.Status.SUCCEEDED;
                        synchronized (out) {
                            if (target.getUid() == android.os.Process.SYSTEM_UID) { if (ok) out.systemOk++; else if (out.message == null) out.message = result.status() + (result.message() != null ? ": " + result.message() : ""); }
                            else if (ok) out.appOk++;
                        }
                        Log.i(TAG, "reload " + target.getProcessName() + " -> " + result.status() + (result.message() != null ? " " + result.message() : ""));
                        if (left.decrementAndGet() == 0) main.post(() -> done.onDone(out));
                    });
                } catch (Throwable e) {
                    Log.w(TAG, "reload " + t.getProcessName() + ": " + e);
                    if (left.decrementAndGet() == 0) main.post(() -> done.onDone(out));
                }
            }
        }, "identra-reload").start();
    }
}
