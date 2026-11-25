package dev.identra.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import org.luaj.vm2.Globals;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.Varargs;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import dev.identra.xposed.IXposedHookLoadPackage;
import dev.identra.xposed.IXposedHookZygoteInit;
import dev.identra.xposed.XC_MethodHook;
import dev.identra.xposed.XposedBridge;
import dev.identra.xposed.callbacks.XC_LoadPackage;
import dev.identra.app.loggers.LogHelper;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.file.FileEx;
import dev.identra.app.x.file.ModePermission;
import dev.identra.app.x.hook.HookCore;
import dev.identra.app.x.hook.filter.HookRepository;
import dev.identra.app.hooks.XHookUtil;

//package dev.identra.app;

import java.util.Collection;

import dev.identra.app.hooks.LuaHookWrapper;
import dev.identra.app.hooks.LuaScriptHolder;
import dev.identra.app.hooks.XReporter;
import dev.identra.app.hooks.LuaHookResolver;
import dev.identra.app.logger.XLog;
import dev.identra.app.logger.XReport;

import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.utilities.ReflectUtilEx;
import dev.identra.app.x.hook.inlined.HashMapHooks;
import dev.identra.app.x.hook.inlined.UpTimeHooks;
import dev.identra.app.x.hook.interceptors.cell.PhoneServicesHook;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.XposedUtility;
import dev.identra.app.x.core.commands.GlobalCommandBridge;
import dev.identra.app.x.core.commands.call.GetBridgeVersionCommand;
import dev.identra.app.x.core.commands.query.GetAssignedHooksExCommand;
import dev.identra.app.x.core.database.DatabasePathUtil;
import dev.identra.app.x.core.hook.PackageHookContext;
import dev.identra.app.x.core.settings.GroupStats;

/*
    ToDO:
        [>] Add the remaining remapped Entries
        [>] Ensure use Proper abstraction, Returning Collection is useless as Caller function Can cast to get it as Collection so have it return something like list
        [>] When doing "toString" function ensure the "appendFieldLine" takes in the Constant for the Field Name if available
        [>] Make a System that for one compresses more of the fragment bullshit
        [>] Make a Application shared system
        [>] Clean up shared
        [>] Make a System for like "onDataEvent(Object o)"
 */

public class Identra implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    private static final String TAG = LibUtil.generateTag(Identra.class);
    public XReporter report = new XReporter();
    public static int version = -1;

    // ---- 0.6.1 hot reload: what a new module generation needs to take over (framework objects only) ----
    /** system_server: context found at systemReady, class loaders of system_server and the settings provider. */
    public static volatile Context sysContext;
    public static volatile ClassLoader androidLoader, settingsLoader;
    /** system_server: package listeners this generation registered (unregistered before a reload). */
    public static final java.util.List<android.content.BroadcastReceiver> receivers = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** app process: what hookPackage ran with. */
    public static volatile Context appContext;
    public static volatile ClassLoader appLoader;
    public static volatile String appPackage;
    public static volatile int appUid = -1;

    public void initZygote(final IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        Log.i(TAG, "initZygote system=" + startupParam.startsSystemServer + " debug=" + BuildConfig.DEBUG);
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        int uid = Process.myUid();
        Log.i(TAG, "Loaded " + lpparam.packageName + ":" + uid);
        XposedBridge.log(TAG + " Loaded " + lpparam.packageName + ":" + uid);

        if ("android".equals(lpparam.packageName))
            hookAndroid(lpparam);
        if ("com.android.providers.settings".equals(lpparam.packageName))
            hookSettings(lpparam);
        if (!"android".equals(lpparam.packageName) &&
                !lpparam.packageName.startsWith(BuildConfig.APPLICATION_ID)) {
            hookApplication(lpparam);
        }
    }

    private void hookSettings(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        GetBridgeVersionCommand.init();
        settingsLoader = lpparam.classLoader;
        installSettingsHooks(lpparam.classLoader, lpparam.packageName);
    }

    /** SettingsProvider.call / query carry Identra's command channel (also used after a hot reload). */
    static void installSettingsHooks(ClassLoader loader, final String packageName) throws Throwable {
        // https://android.googlesource.com/platform/frameworks/base/+/master/packages/SettingsProvider/src/com/android/providers/settings/SettingsProvider.java
        Class<?> clsSet = Class.forName("com.android.providers.settings.SettingsProvider", false, loader);
        XposedBridge.hookMethod(
                clsSet.getMethod("call", String.class, String.class, Bundle.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        GlobalCommandBridge.handeCall(param, packageName);
                    }
                });

        // Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
        XposedBridge.hookMethod(
                clsSet.getMethod("query", Uri.class, String[].class, String.class, String[].class, String.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        GlobalCommandBridge.handleQuery(param, packageName);
                    }
                });
    }


    private void hookSubscriptionManagerService(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Direct hook to the SubscriptionManagerService class
            Class<?> subManagerService = Class.forName(
                    "com.android.internal.telephony.subscription.SubscriptionManagerService",
                    false,
                    lpparam.classLoader
            );

            XposedUtility.logI_xposed(TAG, "Successfully found SubscriptionManagerService class");

            // Hook all variants of methods that might handle subscription info
            String[] methodsToHook = {
                    "getActiveSubscriptionInfoList",
                    "getActiveSubscriptionInfo",
                    "getSubscriptionInfo",
                    "getSubscriptionInfoInternal",
                    "getDefaultSubId",
                    "getPhoneId"
            };

            for (String methodName : methodsToHook) {
                XposedBridge.hookAllMethods(subManagerService, methodName, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedUtility.logI_xposed(TAG, "BEFORE [" + methodName + "] Called with " +
                                (param.args.length > 0 ? param.args[0] : "no args"));
                        super.beforeHookedMethod(param);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedUtility.logI_xposed(TAG, "AFTER [" + methodName + "] Result: " + param.getResult());
                        // Here you can modify the result if needed
                        super.afterHookedMethod(param);
                    }
                });
                XposedUtility.logI_xposed(TAG, "Hooked " + methodName + " method");
            }

        } catch (ClassNotFoundException e) {
            XposedUtility.logE_xposed(TAG, "SubscriptionManagerService class not found: " + e.getMessage());

            // Try the alternative approach - hook TelephonyManager directly
            try {
                Class<?> telephonyManagerClass = Class.forName("android.telephony.TelephonyManager", false, lpparam.classLoader);

                // Methods that directly access IMEI or subscription info
                String[] tmMethods = {
                        "getImei",
                        "getDeviceId",
                        "getSubscriberId",
                        "getSimSerialNumber",
                        "getActiveSubscriptionInfoList",
                        "getActiveSubscriptionInfoCount"
                };

                for (String method : tmMethods) {
                    XposedBridge.hookAllMethods(telephonyManagerClass, method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedUtility.logI_xposed(TAG, "TM BEFORE [" + method + "] Called");
                            super.beforeHookedMethod(param);
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            XposedUtility.logI_xposed(TAG, "TM AFTER [" + method + "] Result: " + param.getResult());
                            super.afterHookedMethod(param);
                        }
                    });
                    XposedUtility.logI_xposed(TAG, "Hooked TelephonyManager." + method);
                }
            } catch (Exception te) {
                XposedUtility.logE_xposed(TAG, "Failed to hook TelephonyManager: " + te);
            }
        } catch (Exception e) {
            XposedUtility.logE_xposed(TAG, "Failed to hook SubscriptionManagerService: " + e);
        }
    }

    private void hookAndroid(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // https://android.googlesource.com/platform/frameworks/base/+/master/services/core/java/com/android/server/am/ActivityManagerService.java
        Class<?> clsAM = Class.forName("com.android.server.am.ActivityManagerService", false, lpparam.classLoader);
        XposedBridge.hookAllMethods(clsAM, "systemReady", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    XposedBridge.log(TAG + " Preparing system");
                    Context context = getContext(param.thisObject);
                    sysContext = context;
                    androidLoader = lpparam.classLoader;
                    GetBridgeVersionCommand.init();
                    hookPackage(lpparam, Process.myUid(), context);
                } catch (Throwable ex) {
                    Log.e(TAG, Log.getStackTraceString(ex));
                    XposedBridge.log(ex);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    XposedBridge.log(TAG + " System ready");
                    Context context = getContext(param.thisObject);
                    sysContext = context;
                    onSystemReady(context, param.thisObject.getClass().getClassLoader());
                } catch (Throwable ex) {
                    Log.e(TAG, Log.getStackTraceString(ex));
                    XposedBridge.log(ex);
                }
            }

            @NonNull
            private Context getContext(Object am) throws Throwable {
                // Searching for context
                Context context = null;
                Class<?> cAm = am.getClass();
                while (cAm != null && context == null) {
                    for (Field field : cAm.getDeclaredFields())
                        if (field.getType().equals(Context.class)) {
                            field.setAccessible(true);
                            context = (Context) field.get(am);
                            if(DebugUtil.isDebug())
                                Log.i(TAG, "Context found in " + cAm + " as " + field.getName());

                            break;
                        }
                    cAm = cAm.getSuperclass();
                }
                if (context == null)
                    throw new Throwable("Context not found");

                return context;
            }
        });

        //XposedUtility.logI_xposed(TAG, "Starting Sub Hooks!");
        //hookSubscriptionManagerService(lpparam);
    }



    /** After ActivityManagerService.systemReady (and again after a hot reload of system_server). */
    static void onSystemReady(Context context, ClassLoader serverLoader) throws Throwable {



                    // Store the command protocol version (not the APK versionCode)
                    version = dev.identra.IdentraNs.PROTOCOL;

                    // public static UserManagerService getInstance()
                    Class<?> clsUM = Class.forName("com.android.server.pm.UserManagerService", false, serverLoader);
                    Object um = clsUM.getDeclaredMethod("getInstance").invoke(null);
                    //  public int[] getUserIds()
                    int[] userids = (int[]) um.getClass().getDeclaredMethod("getUserIds").invoke(um);

                    // Listen for package changes
                    for (int userid : userids) {
                        //Log.i(TAG, "Registering package listener user=" + userid);
                        IntentFilter ifPackageAdd = new IntentFilter();
                        ifPackageAdd.addAction(Intent.ACTION_PACKAGE_ADDED);
                        ifPackageAdd.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
                        ifPackageAdd.addDataScheme("package");
                        ReceiverPackage rp = new ReceiverPackage();
                        XUtil.createContextForUser(context, userid).registerReceiver(rp, ifPackageAdd);
                        receivers.add(rp);
                    }
        // 0.6.1: open the database now: runs CollectionMigration and reads the hook definitions of the installed APK
        GlobalCommandBridge.warmUp(context);
    }

    public void hookApplication(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        final int uid = android.os.Process.myUid();
        final boolean tiramisuOrHigher = (Build.VERSION.SDK_INT >= 33);
        // https://android.googlesource.com/platform/frameworks/base/+/169aeafb2d97b810ae123ad036d0c58336961c55%5E%21/#F1
        Class<?> at = Class.forName(tiramisuOrHigher ? "android.app.Instrumentation" : "android.app.LoadedApk", false, lpparam.classLoader);

        XposedBridge.hookAllMethods(at,
                tiramisuOrHigher ? "newApplication" : "makeApplication", new XC_MethodHook() {
                    private boolean made = false;

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            if(!made) {
                                made = true;
                                Context context = (Application) param.getResult();

                                //Check for isolate process
                                int userid = XUtil.getUserId(uid);
                                int start = XUtil.getUserUid(userid, 99000);
                                int end = XUtil.getUserUid(userid, 99999);
                                boolean isolated = (uid >= start && uid <= end);
                                if (isolated) {
                                    Log.i(TAG, "Skipping isolated " + lpparam.packageName + ":" + uid);
                                    return;
                                }

                                if(lpparam.packageName.equalsIgnoreCase("com.android.phone")) {
                                    //XposedUtility.logI_xposed(TAG, "!!! Phone App! =" + lpparam.packageName);
                                    //PhoneServicesHook.deployHook(lpparam, context);
                                } else {
                                    appContext = context;
                                    appLoader = lpparam.classLoader;
                                    appPackage = lpparam.packageName;
                                    appUid = uid;
                                    hookPackage(lpparam, uid, context);
                                }
                            }
                        }catch (Throwable ex) {
                            Log.e(TAG, Log.getStackTraceString(ex));
                            XposedBridge.log(ex);
                        }
                    }
                });
    }

    public void hookPackage(final XC_LoadPackage.LoadPackageParam lpparam, int uid, final Context context) throws Throwable {
        if(DebugUtil.isDebug())
            Log.d(TAG, "Starting Initializing Hooks for App: " + lpparam.packageName + " Uid: " + uid);
        HookCore.initHooks(lpparam, uid, context);
    }

    // ---- 0.6.1 hot reload ---------------------------------------------------------------

    /** Old generation, just before the framework swaps in new code: stop what we own. */
    public static void retireForReload() {
        try {
            Context c = sysContext;
            for (android.content.BroadcastReceiver r : receivers) {
                try { if (c != null) c.unregisterReceiver(r); } catch (Throwable ignored) { }
            }
            receivers.clear();
        } catch (Throwable ignored) { }
        if (sysContext != null) { try { GlobalCommandBridge.shutdown(); } catch (Throwable ignored) { } }
        try { dev.identra.app.found.FoundObserver.shutdown(); } catch (Throwable ignored) { }
        try { dev.identra.app.nativeprops.NativeProps.remove(); } catch (Throwable ignored) { }
    }

    /** New generation in system_server: install the command channel, listeners and database again. */
    public static void reloadSystem(Context context, ClassLoader android, ClassLoader settings) throws Throwable {
        sysContext = context;
        androidLoader = android;
        settingsLoader = settings;
        GetBridgeVersionCommand.init();
        if (settings != null) installSettingsHooks(settings, "com.android.providers.settings");
        XC_LoadPackage.LoadPackageParam lp = new XC_LoadPackage.LoadPackageParam();
        lp.packageName = "android";
        lp.processName = "android";
        lp.classLoader = android;
        try { HookCore.initHooks(lp, Process.myUid(), context); } catch (Throwable t) { Log.w(TAG, "system hooks: " + t); }
        onSystemReady(context, android);
        Log.i(TAG, "system part reloaded, build " + BuildConfig.VERSION_CODE);
    }

    /** New generation in an app process: deploy the app's hooks again with the new code. */
    public static void reloadApp(Context context, ClassLoader loader, String packageName, int uid) throws Throwable {
        appContext = context;
        appLoader = loader;
        appPackage = packageName;
        appUid = uid;
        XC_LoadPackage.LoadPackageParam lp = new XC_LoadPackage.LoadPackageParam();
        lp.packageName = packageName;
        lp.processName = packageName;
        lp.classLoader = loader;
        HookCore.initHooks(lp, uid, context);
        Log.i(TAG, packageName + " reloaded, build " + BuildConfig.VERSION_CODE);
    }
}
