package dev.identra.app;

import android.util.Log;

import androidx.annotation.NonNull;

import dev.identra.xposed.IXposedHookZygoteInit;
import dev.identra.xposed.XposedBridge;
import dev.identra.xposed.callbacks.XC_LoadPackage;
import io.github.libxposed.api.XposedModule;

/**
 * libxposed API 102 entry point (Identra).
 * Attaches the framework to the compat bridge (dev.identra.xposed) and drives
 * the existing Identra hook engine from the modern lifecycle callbacks:
 *   onSystemServerStarting -> handleLoadPackage("android")
 *   onPackageReady         -> handleLoadPackage(app)
 * 0.6.1: hot reload (module.prop autoHotReload=true, or the app's request through XposedService).
 *   onHotReloading (old code)  -> stop listeners/threads, hand over class loaders + context
 *   onHotReloaded  (new code)  -> install the new hooks first, then remove the old ones
 * No reboot is ever needed for an update once a generation with these callbacks is running.
 */
public class XposedEntry extends XposedModule {
    private static final String TAG = "Identra";
    private final Identra xlua = new Identra();
    private String processName = null;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        XposedBridge.attach(this);
        processName = param.getProcessName();
        Log.i(TAG, "module loaded in " + processName + " api=" + getApiVersion()
                + " fw=" + getFrameworkName() + " " + getFrameworkVersion());
        try {
            IXposedHookZygoteInit.StartupParam sp = new IXposedHookZygoteInit.StartupParam();
            sp.startsSystemServer = param.isSystemServer();
            sp.modulePath = getModuleApplicationInfo().sourceDir;
            dev.identra.app.nativeprops.NativeProps.modulePath = sp.modulePath;
            xlua.initZygote(sp);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        XC_LoadPackage.LoadPackageParam lp = new XC_LoadPackage.LoadPackageParam();
        lp.packageName = "android";
        lp.processName = "android";
        lp.classLoader = param.getClassLoader();
        lp.isFirstApplication = true;
        dispatch(lp);
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        XC_LoadPackage.LoadPackageParam lp = new XC_LoadPackage.LoadPackageParam();
        lp.packageName = param.getPackageName();
        lp.processName = processName;
        lp.classLoader = param.getClassLoader();
        lp.appInfo = param.getApplicationInfo();
        lp.isFirstApplication = param.isFirstPackage();
        dispatch(lp);
    }

    private void dispatch(XC_LoadPackage.LoadPackageParam lp) {
        try {
            xlua.handleLoadPackage(lp);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    // ---- 0.6.1 hot reload ----

    @Override
    public boolean onHotReloading(@NonNull HotReloadingParam param) {
        try {
            Object[] state;
            if (dev.identra.app.Identra.sysContext != null) {
                state = new Object[]{"system", dev.identra.app.Identra.sysContext,
                        dev.identra.app.Identra.androidLoader, dev.identra.app.Identra.settingsLoader};
            } else if (dev.identra.app.Identra.appContext != null) {
                state = new Object[]{"app", dev.identra.app.Identra.appContext, dev.identra.app.Identra.appLoader,
                        dev.identra.app.Identra.appPackage, Integer.valueOf(dev.identra.app.Identra.appUid)};
            } else {
                state = new Object[]{"none"};
            }
            dev.identra.app.Identra.retireForReload();
            param.setSavedInstanceState(state);
            Log.i(TAG, "hot reload accepted in " + processName + " (" + state[0] + ")");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "hot reload refused in " + processName + ": " + t);
            return false;
        }
    }

    @Override
    public void onHotReloaded(@NonNull HotReloadedParam param) {
        XposedBridge.attach(this);
        try { dev.identra.app.nativeprops.NativeProps.modulePath = getModuleApplicationInfo().sourceDir; } catch (Throwable ignored) { }
        processName = param.getProcessName();
        boolean ok = false;
        try {
            Object st = param.getSavedInstanceState();
            if (st instanceof Object[]) {
                Object[] a = (Object[]) st;
                String kind = (String) a[0];
                if ("system".equals(kind)) {
                    dev.identra.app.Identra.reloadSystem((android.content.Context) a[1], (ClassLoader) a[2], (ClassLoader) a[3]);
                    ok = true;
                } else if ("app".equals(kind)) {
                    dev.identra.app.Identra.reloadApp((android.content.Context) a[1], (ClassLoader) a[2], (String) a[3], (Integer) a[4]);
                    ok = true;
                } else {
                    ok = true;      // nothing was hooked in this process yet
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "hot reload in " + processName + " failed: " + Log.getStackTraceString(t));
        }
        // new hooks are in place (or the process had none): drop the old generation's hooks.
        // If installing failed, keep the old hooks running rather than leave the process unprotected.
        if (ok) {
            for (io.github.libxposed.api.XposedInterface.HookHandle h : param.getOldHookHandles()) {
                try { h.unhook(); } catch (Throwable ignored) { }
            }
        }
        Log.i(TAG, "hot reloaded in " + processName + " ok=" + ok + " build=" + dev.identra.app.BuildConfig.VERSION_CODE);
    }

}
