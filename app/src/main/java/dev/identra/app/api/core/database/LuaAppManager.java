package dev.identra.app.api.core.database;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.identra.app.XDatabaseOld;
import dev.identra.app.XLegacyCore;

import dev.identra.app.XUtil;
import dev.identra.app.api.XResult;
import dev.identra.app.api.app.LuaSimplePacket;
import dev.identra.app.api.hook.assignment.IdentraAssignmentDataHelper;
import dev.identra.app.api.hook.assignment.LuaAssignment;
import dev.identra.app.api.settings.LuaSetting;
import dev.identra.app.api.settings.LuaSettingExtended;
import dev.identra.app.api.core.provider.IdentraAppProvider;
import dev.identra.app.api.core.provider.IdentraHookProvider;
import dev.identra.app.api.xstandard.database.DatabaseHelp;
import dev.identra.app.api.xstandard.database.SqlQuerySnake;

public class LuaAppManager {
    private static final String TAG = "Identra.XAppDatabase";

    public static boolean initAppAssignments(Context context, XDatabaseOld db, Integer user, String category, Boolean kill) { return initAppAssignments(context, db, LuaSimplePacket.create(user, category, kill)); }
    public static boolean initAppAssignments(Context context, XDatabaseOld db, LuaSimplePacket packet) {
        packet.resolveUserID();
        List<String> collection = IdentraHookProvider.getCollections(context, db, packet.getUser());
        List<String> hookIds = new ArrayList<>();//XLegacyCore.getHookIds(packet.getCategory(), collection);
        IdentraAssignmentDataHelper assignmentData = new IdentraAssignmentDataHelper(packet.getCategory(), packet.getOriginalUser());

        try {
            if(!db.beginTransaction(true))
                return false;

            for(String hookId : hookIds) {
                if(!db.insert(LuaAssignment.Table.name, assignmentData.createContentValues(hookId))) {
                    Log.e(TAG, "Error Inserting Assignment , hookId=" + hookId);
                    //throw new Throwable("Error inserting assignment");
                }
            }

            db.setTransactionSuccessful();
        }finally {
            db.endTransaction(true, false);
        }

        if (packet.isKill())
            IdentraAppProvider.forceStop(context, packet.getCategory(), packet.getUser());

        Log.i(TAG, "Init app pkg=" + packet.getCategory() + " uid=" + packet.getUser() + " assignments=" + hookIds.size());
        return true;
    }

    public static boolean clearApp(Context context, XDatabaseOld db, Integer user, String category, Boolean kill, Boolean deleteAllData) { return clearApp(context, db, LuaSimplePacket.create(user, category, kill, deleteAllData)); }
    public static boolean clearApp(Context context, XDatabaseOld db, LuaSimplePacket packet)  {
        packet.resolveUserID();
        if(!DatabaseHelp.deleteItem(packet.createUserQuery(db, LuaAssignment.Table.name)))
            return false;

        if(packet.isDeleteFullData())
            if(!DatabaseHelp.deleteItem(packet.createUserQuery(db, LuaSetting.Table.NAME)))
                return false;

        if (packet.isKill())
            IdentraAppProvider.forceStop(context, packet.getCategory(), packet.getUser());

        return true;
    }

    public static XResult clearSettings(String packageName, XDatabaseOld db) {
        Collection<LuaSettingExtended> settings = new SqlQuerySnake(db, LuaSetting.Table.NAME)
                .whereColumn(LuaSetting.Table.FIELD_CATEGORY, packageName)
                .queryAll(LuaSettingExtended.class, true);

        if(settings != null && settings.size() > 0) {
            for(LuaSettingExtended s : settings) {
                SqlQuerySnake query = SqlQuerySnake.create(db, LuaSetting.Table.NAME)
                        .whereColumn(LuaSettingExtended.Table.FIELD_NAME, s.getName())
                        .whereColumn(LuaSetting.Table.FIELD_CATEGORY, packageName);

                DatabaseHelp.deleteItem(query);
            }
        }

        return XResult.create().setSucceeded();
    }

    public static boolean clearData(int userid, XDatabaseOld db)  {
        Log.i(TAG, "Clearing data user=" + userid);

        boolean result;
        if(userid == 0) {
            //if 0 drop the whole table
            db.beginTransaction(true);
            result = db.delete(LuaAssignment.Table.name) && db.delete(LuaSetting.Table.NAME);
            db.endTransaction(true, true);
        }else {
            int start = XUtil.getUserUid(userid, 0);
            int end = XUtil.getUserUid(userid, Process.LAST_APPLICATION_UID);

            result = DatabaseHelp.deleteItem(
                    SqlQuerySnake
                            .create(db, LuaAssignment.Table.name)
                            .whereColumn("uid", start, ">=")
                            .whereColumn("uid", end, "<="));

            result = result && DatabaseHelp.deleteItem(
                    SqlQuerySnake
                            .create(db, LuaSetting.Table.NAME)
                            .whereColumn("user", userid));
        }

        return result;
    }
}
