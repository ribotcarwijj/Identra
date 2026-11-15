package dev.identra.app.api.core.provider;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Binder;
import android.os.Process;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.identra.app.BuildConfig;
import dev.identra.app.DebugUtil;
import dev.identra.app.XDatabaseOld;
import dev.identra.app.XLegacyCore;
import dev.identra.app.api.XResult;

import dev.identra.app.XUtil;
import dev.identra.app.XposedUtil;
import dev.identra.app.api.hook.assignment.LuaAssignment;
import dev.identra.app.api.hook.assignment.LuaAssignmentWriter;
import dev.identra.app.api.settings.LuaSetting;
import dev.identra.app.api.xstandard.UserIdentityPacket;
import dev.identra.app.api.xstandard.database.SqlQuerySnake;
import dev.identra.app.api.app.IdentraApp;
import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.x.runtime.RuntimeUtils;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;

public class IdentraAppProvider {
    private static final String TAG = "Identra.XAppProvider";
    public static int getVersion(Context context) throws Throwable {
        if (XposedUtil.isVirtualXposed()) {
            PackageInfo pi = context.getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID, 0);
            return pi.versionCode;
        } else
            return -55;
    }

    public static boolean clearAppData(Context context, String packageName) {
        if(packageName.equalsIgnoreCase(UserIdentityPacket.GLOBAL_NAMESPACE)) {
            return false;
        }

        Log.i(TAG, "Clearing App Data for Package: " + packageName);
        try {
            long identity = Binder.clearCallingIdentity();
            try {
                //android.content.pm.IPackageDataObserver java
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                Method mForceStop = am.getClass().getMethod("clearApplicationUserData", String.class, Class.forName("android.content.pm.IPackageDataObserver"));
                mForceStop.invoke(am, packageName, null);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear App Data for Package:" + packageName + " error=" + e + " Stack=" + RuntimeUtils.getStackTraceSafeString(e));
                return false;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }catch (Exception e) {
            return false;
        }
    }

    public static boolean forceStop(Context context, String packageName, int userid) { return forceStop(context, packageName, userid, null); }
    public static boolean forceStop(Context context, String packageName, int userid, XResult res) {
        if(packageName.equalsIgnoreCase(UserIdentityPacket.GLOBAL_NAMESPACE)) {
            XResult.logError(TAG, res, "Cannot Kill Global or UID: " + userid + " pgk=" + packageName);
            return false;
        }

        Log.i(TAG, "forceStop on package=" + packageName + " userid=" + userid);
        try {
            // Access activity manager as system user
            long identity = Binder.clearCallingIdentity();
            try {
                // public void forceStopPackageAsUser(String packageName, int userId)
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                Method mForceStop = am.getClass().getMethod("forceStopPackageAsUser", String.class, int.class);
                mForceStop.invoke(am, packageName, userid);
                return true;
            } catch (Exception e) {
                XResult.logError(TAG, res, "Failed to kill user [forceStopPackageAsUser] user=" + userid + "pkg=" + packageName + " er=" + e);
                return false;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }catch (Exception e) {
            XResult.logError(TAG, res, "Failed to kill [forceStopPackageAsUser] user=" + userid + "pkg=" + packageName + " er=" + e);
            return false;
        }
    }


    public static IdentraApp getApp(Context context, XDatabaseOld db, int cuid, String packageName, boolean initForceStop, boolean initSettings) {
        long identity = Binder.clearCallingIdentity();
        int userid = XUtil.getUserId(cuid);
        try {
            Context contextForUser = XUtil.createContextForUser(context, userid);
            PackageManager pm = contextForUser.getPackageManager();

            Log.i(TAG, "getApp for Package=" + packageName + " uid=" + userid + " cuid=" + cuid + " initForceStop=" + initForceStop + " initSettings=" + initSettings);

            for (ApplicationInfo ai : XposedUtil.getApplications(contextForUser))
                if (ai.packageName.equalsIgnoreCase(packageName)) {
                    try {
                        Log.i(TAG, "Found package for GetApp: " + packageName);


                        int enabledSetting = pm.getApplicationEnabledSetting(ai.packageName);
                        boolean enabled = (ai.enabled &&
                                (enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
                                        enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
                        boolean persistent = ((ai.flags & ApplicationInfo.FLAG_PERSISTENT) != 0 ||
                                "android".equals(ai.packageName));
                        boolean system = ((ai.flags &
                                (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0);

                        Log.i(TAG, "Created the app shit almost.. " + packageName);
                        IdentraApp app = new IdentraApp();
                        app.setUid(ai.uid);
                        app.setPackageName(ai.packageName);
                        app.setIcon(ai.icon);
                        app.setLabel((String) pm.getApplicationLabel(ai));
                        app.setEnabled(enabled);
                        app.setPersistent(persistent);
                        app.setSystem(system);
                        app.setForceStop((!persistent && !system));
                        app.setAssignments(new ArrayList<LuaAssignment>());

                        Log.i(TAG, "Created IdentraApp Object pkg=" + packageName + " o=" + app);

                        final HashMap<String, IdentraApp> apps = new HashMap<>();
                        apps.put(app.getPackageName(), app);
                        if(initForceStop) initAppForceToStop(apps, db, userid);
                        if(initSettings) initAppDatabaseSettings(context, db, apps, userid);

                        Log.i(TAG, "Settings=" + app.getAssignments().size());
                        return app;
                    } catch (Throwable ex) {
                        Log.e(TAG, ex + "\n" + Log.getStackTraceString(ex));
                    }
                }else {
                    //Log.w(TAG, "Not Pkg=" + ai.packageName);
                }
        }catch (Throwable e) {
            Log.e(TAG, "Failed to get App: pkg=" + packageName + " uid=" + userid + " e=" + e + "\n" + Log.getStackTraceString(e));
        }finally {
            Binder.restoreCallingIdentity(identity);
        } return new IdentraApp();
    }

    public static Map<String, IdentraApp> getApps(Context context, XDatabaseOld db, int cuid, boolean initForceToStop, boolean initSettings) {
        int userid = XUtil.getUserId(cuid);
        Map<String, IdentraApp> apps = new HashMap<>();

        long identity = Binder.clearCallingIdentity();
        try {
            //https://github.com/zer0def/vXPrivacyLua/commit/c030dd1880afda81b989185f8e2527c8358db799
            //For TaiChi support
            Context contextForUser = XUtil.createContextForUser(context, userid);
            PackageManager pm = contextForUser.getPackageManager();

            for (ApplicationInfo ai : XposedUtil.getApplications(contextForUser))
                if (!ai.packageName.startsWith(BuildConfig.APPLICATION_ID))
                    try {
                        int enabledSetting = pm.getApplicationEnabledSetting(ai.packageName);
                        boolean enabled = (ai.enabled &&
                                (enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
                                        enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
                        boolean persistent = ((ai.flags & ApplicationInfo.FLAG_PERSISTENT) != 0 ||
                                "android".equals(ai.packageName));
                        boolean system = ((ai.flags &
                                (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0);

                        IdentraApp app = new IdentraApp();
                        app.setUid(ai.uid);
                        app.setPackageName(ai.packageName);
                        app.setIcon(ai.icon);
                        app.setLabel((String) pm.getApplicationLabel(ai));
                        app.setEnabled(enabled);
                        app.setPersistent(persistent);
                        app.setSystem(system);
                        app.setForceStop((!persistent && !system));
                        app.setAssignments(new ArrayList<LuaAssignment>());
                        apps.put(app.getPackageName(), app);
                    } catch (Throwable ex) {
                        Log.e(TAG, ex + "\n" + Log.getStackTraceString(ex));
                    }
        }catch (Throwable e) {
            Log.e(TAG, "Failed to get Apps: " + e + "\n" + Log.getStackTraceString(e));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }

        if(DebugUtil.isDebug())
            Log.i(TAG, "Installed apps=" + apps.size() + " userId=" + userid + " cuid=" + cuid);

        if(initForceToStop)
            initAppForceToStop(apps, db, userid);

        if(initSettings)
            initAppDatabaseSettings(context, db, apps, userid);
            //initAppDatabaseSettings(context, apps, db, userId);

        return apps;
    }

    private static void initAppForceToStop(Map<String, IdentraApp> apps, XDatabaseOld db, int userid) {
        //direct insert here



        SqlQuerySnake snake = SqlQuerySnake
                .create(db, LuaSetting.Table.NAME)
                .whereColumn("user", Integer.toString(userid))
                .whereColumn("name", "forcestop")
                .onlyReturnColumns("category", "value");


        /*cursor = db.query(
                "setting",
                new String[]{"category", "value"},
                "user = ? AND name = 'forcestop'",
                new String[]{Integer.toString(userid)},
                null, null, null);*/

        db.readLock();
        Cursor c = snake.query();
        try {
            while (c.moveToNext()) {
                String pkg = c.getString(0);
                if (apps.containsKey(pkg)) {
                    IdentraApp app = apps.get(pkg);
                    if(app != null) {
                        app.setForceStop(Boolean.parseBoolean(c.getString(1)));
                        if(apps.size() == 1)
                            return;
                    }
                } else
                    Log.i(TAG, "Package " + pkg + " not found (force stop)");
            }
        }finally {
            snake.clean(c);
            db.readUnlock();
        }
    }

    private static void initAppDatabaseSettings(Context context, XDatabaseOld db, Map<String, IdentraApp> apps, int userid) {
        int start = XUtil.getUserUid(userid, 0);
        int end = XUtil.getUserUid(userid, Process.LAST_APPLICATION_UID);

        SqlQuerySnake snake = SqlQuerySnake
                .create(db, LuaAssignment.Table.name)
                .onlyReturnColumns("package", "uid", "hook", "installed", "used", "restricted", "exception")
                .whereColumn("uid", start, ">=")
                .whereColumn("uid", end, "<=");

        List<String> collections = IdentraHookProvider.getCollections(context, db, userid);

        db.readLock();
        Cursor c = snake.query();
        try {
            int colPkg = c.getColumnIndex("package");
            int colUid = c.getColumnIndex("uid");
            int colHook = c.getColumnIndex("hook");
            int colInstalled = c.getColumnIndex("installed");
            int colUsed = c.getColumnIndex("used");
            int colRestricted = c.getColumnIndex("restricted");
            int colException = c.getColumnIndex("exception");

            while (c.moveToNext()) {
                String pkg = c.getString(colPkg);
                int uid = c.getInt(colUid);
                String hookId = c.getString(colHook);

                if (apps.containsKey(pkg)) {
                    IdentraApp app = apps.get(pkg);
                    if(app == null)
                        continue;

                    if (app.getUid() != uid)
                        continue;

                    XHook hook = XLegacyCore.getHook(hookId, pkg, collections);
                    if(hook != null) {
                        //LuaAssignmentWriter assignment = new LuaAssignmentWriter(hook);
                        //assignment.setInstalled(c.getLong(colInstalled));
                        //assignment.setUsed(c.getLong(colUsed));
                        //assignment.setRestricted((c.getInt(colRestricted) == 1));
                        //assignment.setException(c.getString(colException));
                        //app.addAssignment(assignment);
                    }

                    //if(apps.size() == 1) {
                    //    Log.i(TAG, "Returning from Init Setting sfrom app as it is one");
                    //    return;
                    //}

                } else
                    Log.i(TAG, "Package " + pkg + " not found");
            }
        }finally {
            snake.clean(c);
            db.readUnlock();
        }
    }
}
