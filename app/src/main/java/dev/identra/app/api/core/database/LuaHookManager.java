package dev.identra.app.api.core.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


import dev.identra.app.DebugUtil;
import dev.identra.app.XDatabaseOld;
import dev.identra.app.XLegacyCore;
import dev.identra.app.XUtil;
import dev.identra.app.api.XResult;
import dev.identra.app.api.hook.assignment.LuaAssignment;
import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.api.hook.assignment.LuaAssignmentPacket;
import dev.identra.app.api.core.provider.IdentraAppProvider;
import dev.identra.app.api.xstandard.database.DatabaseHelp;
import dev.identra.app.api.xstandard.database.SqlQuerySnake;
import dev.identra.app.api.hook.assignment.IdentraAssignmentDataHelper;
import dev.identra.app.api.hook.group.IdentraGroupDataHelper;
import dev.identra.app.hooks.XReport;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;

public class LuaHookManager {
    private static final String TAG = "Identra.XHookDatabase";

    public static XResult updateHook(XDatabaseOld db, IdentraHook hook, String extraId) {
        return XResult.create().setMethodName("updateHook")
                .log("[updating hook] extra id=" + extraId, TAG)
                                .setResult(hook == null ? deleteHook(db, extraId) : hook.isBuiltin() || putHook(db, hook));
    }

    public static boolean putHook(XDatabaseOld db, IdentraHook hook) {
        //Make sure we do not need to prepare db what not
        return DatabaseHelp.insertItem(db, IdentraHook.Table.name, hook);
    }

    public static boolean deleteHook(XDatabaseOld db, String id) { return DatabaseHelp.deleteItem(SqlQuerySnake.create(db, IdentraHook.Table.name).whereColumn("id", id)); }

    public static XResult assignHooks(Context context, XDatabaseOld db, LuaAssignmentPacket packet) {
        String packageName = packet.getCategory();
        int uid = packet.getUser();

        XResult res = XResult.create().setMethodName("assignHooks").setExtra(packet.toString());
        //Assign Hook(s) to a App (package name, uid)
        List<String> groups = new ArrayList<>();
        IdentraAssignmentDataHelper assignmentData = new IdentraAssignmentDataHelper(packageName, uid);
        IdentraGroupDataHelper groupData = new IdentraGroupDataHelper(packageName, uid);

        try {
            if(!db.beginTransaction(true))
                return res.setFailed("Cannot being Database Transaction");

            if(!db.hasTable(LuaAssignment.Table.name)) {
                Log.e(TAG, "Table does not exist [" + LuaAssignment.Table.name + "] in Database [" + db + "]");
                return res.setFailed("Assignments Table does not exist!");
            }

            int failed = 0;
            int succeeded = 0;
            for(String hookId : packet.getHookIds()) {
                XHook hook = XLegacyCore.getHook(hookId);

                //Add its Group to the group list
                if (hook != null && !groups.contains(hook.group))
                    groups.add(hook.group);

                if(packet.isDelete()) {
                    Log.i(TAG, packageName + ":" + uid + "/" + hookId + " deleted");
                    if(!db.delete(LuaAssignment.Table.name, assignmentData.getSelectionArgs(), assignmentData.createValueArgs(hookId))) {
                        Log.e(TAG, "Failed to Delete Assignment ID=" + hookId);
                        failed++;
                    }else succeeded++;
                }else {
                    Log.i(TAG, packageName + ":" + uid + "/" + hookId + " added");
                    if(!db.insert(LuaAssignment.Table.name, assignmentData.createContentValues(hookId))) {
                        Log.e(TAG, "Failed to Insert Assignment ID=" + hookId);
                        failed++;
                    }else succeeded++;
                }
            }

            if (!packet.isDelete())
                for (String group : groups) {
                    if(!db.delete("`group`", groupData.getSelectionArgs(), groupData.createValueArgs(group))) {
                        Log.e(TAG, "Failed to Delete Group=" + group);
                        failed++;
                    }else succeeded++;
                }

            Log.i(TAG, "assignHooks succeeded operations=" + succeeded + " failed operations=" + failed);
            if(succeeded == 0 && !packet.getHookIds().isEmpty())
                return res.setFailed("None of the Operations succeeded ! (all failed)");

            db.setTransactionSuccessful();
        }finally {
            db.endTransaction(true, false);
        }

        if(DebugUtil.isDebug())
            Log.d(TAG, "Force Stop Flag ? " + (packet.isKill()) + " Pkg=" + packageName + " UserId=" + XUtil.getUserId(uid) + " Uid=" + uid);

        if (packet.isKill())
            IdentraAppProvider.forceStop(context, packageName, XUtil.getUserId(uid));

        return res.setSucceeded();
    }

    public static long report(XReport report, IdentraHook hook, XDatabaseOld db) {
        Log.i(TAG , "Updating Assignment: " + report);

        long used = -1;
        if(!DatabaseHelp.updateItem(db, LuaAssignment.Table.name, report.generateQuery(), report))
            Log.w(TAG, "Error updating Assignment: " + report);

        //Update Group
        if(hook != null && report.event.equals("use") && report.getRestricted() == 1 && report.getNotify(db)) {
            used = SqlQuerySnake
                    .create(db , "`group`")
                    .whereColumns("package", "uid", "name")
                    .whereColumnValues(report.packageName, Integer.toString(report.uid), hook.getGroup())
                    .queryGetFirstLong("used", true);
            //Integer.toString(report.uid)

            Log.i(TAG, " used count=" + used + " hook group=" + hook.getGroup() + " pkg=" + report.packageName + " uid=" + report.uid + " uid resolved=" + Integer.toString(report.getUserId()) + " time=" + report.time);

            //ahh fix this ugly code
            //dupes too many standardize query snake
            //if(!DatabaseHelp.insertItem(db, "`group`",  report.createGroupObject(hook, used)))
            //    Log.e(TAG, "Error inserting group: " + report);

            db.beginTransaction(true);

            ContentValues cv = new ContentValues();
            cv.put("package", report.packageName);
            cv.put("uid", report.uid);
            cv.put("name", hook.getGroup());
            cv.put("used", report.time);
            long rows = db.getDatabase().insertWithOnConflict("`group`", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            if (rows < 0)
                Log.e(TAG, "Error inserting group: " + report);


            Log.i(TAG, "Updated Group, returned=" + rows);
            db.endTransaction(true, !(rows < 0));
            //throw new Throwable("Error inserting group");
        }

        Log.i(TAG, "Report returning: " + used);
        return used;
    }
}
