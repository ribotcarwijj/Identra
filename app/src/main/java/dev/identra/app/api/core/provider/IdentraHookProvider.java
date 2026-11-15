package dev.identra.app.api.core.provider;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.identra.xposed.XposedBridge;
import dev.identra.app.BuildConfig;
import dev.identra.app.DebugUtil;
import dev.identra.app.XDatabaseOld;
import dev.identra.app.XLegacyCore;
import dev.identra.app.XUiGroup;
import dev.identra.app.api.XResult;
import dev.identra.app.api.hook.LuaHookPacket;
import dev.identra.app.api.xmock.database.LuaSettingsManager;
import dev.identra.app.api.xstandard.UserIdentityPacket;
import dev.identra.app.api.core.IdentraCall;

import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.api.core.call.GetVersionCommand;
import dev.identra.app.api.core.database.LuaHookManager;
import dev.identra.app.utilities.StringUtil;

public class IdentraHookProvider {
    private static final String TAG = "Identra.XHookProvider";

    public static List<String> getCollections(Context context, XDatabaseOld db, int userId) {
        //check this
        String value = LuaSettingsManager.getSettingValue(context, db, "collection",  userId, UserIdentityPacket.GLOBAL_NAMESPACE);
        List<String> result = new ArrayList<>();

        if(DebugUtil.isDebug())
            Log.i(TAG, "collection=" + value);

        if(!StringUtil.isValidString(value)) {
            value = LuaSettingsManager.DEFAULT_COLLECTIONS;
            LuaSettingsManager.putSetting(context, db, "collection", value);
        }

        if(value.contains(",")) Collections.addAll(result, value.split(","));
        else result.add(value);

        if(DebugUtil.isDebug())
            Log.i(TAG, "collection size=" + result.size());

        return result;
    }

    public static XResult putHook(Context context, XDatabaseOld database, LuaHookPacket packet) throws Throwable { return putHook(context, database, packet.getId(), packet.getDefinition());  }
    public static XResult putHook(Context context, XDatabaseOld database, String id, String definition) throws Throwable {
        XResult res = XResult.create().setMethodName("putHook").setExtra("id=" + id);
        if (!StringUtil.isValidString(id))
            return res.appendErrorMessage("ID Missing from Hook!", TAG).setFailed();

        IdentraHook hook = null;
        if(definition != null) {
            hook = new IdentraHook();
            hook.fromJSONObject(new JSONObject(definition));
        }

        if(hook != null) {
            hook.validate();
            if(!id.equals(hook.getObjectId())) {
                return res.appendErrorMessage("ID Mismatch: Given=" + id + "  Parsed=" + hook.getObjectId(), TAG).setFailed();
            }
        }

        //if(!XLegacyCore.updateHookCache(context, hook, id))
        //    return res.appendErrorMessage("Failed at Updating Hook Cache, id=" + id, TAG).setFailed();

        return LuaHookManager.updateHook(database, hook, id);
    }

    /**
     * The system_server side answers on the command channel (any Identra build). Whether it runs the
     * current code is {@link #isOutdated}; an outdated side still answers, so the UI can work and
     * show a restart notice instead of "not active".
     */
    public static boolean isAvailable(Context context) {
        return systemVersion(context) > 0;
    }

    /** system_server answers, but with an older command protocol: a framework restart is needed. */
    public static boolean isOutdated(Context context) {
        int v = systemVersion(context);
        return v > 0 && v < dev.identra.IdentraNs.PROTOCOL;
    }

    /** getVersion answer of the system_server side (IdentraNs.PROTOCOL, or a versionCode for builds before 0.1.4); -1 if none. */
    public static int systemVersion(Context context) {
        try {
            Bundle b = GetVersionCommand.invoke(context);
            return b == null ? -1 : b.getInt("version", -1);
        } catch (Throwable ex) {
            Log.e(TAG, Log.getStackTraceString(ex));
            XposedBridge.log(ex);
            return -1;
        }
    }

    public static List<XUiGroup> getUiGroups(Context context) {
        List<String> groups = IdentraCall.getGroups(context);
        List<XUiGroup> uiGroups = new ArrayList<>();

        if(groups == null)
            return uiGroups;

        Resources res = context.getResources();
        for(String name : groups) {
            String g = name.toLowerCase().replaceAll("[^a-z]", "_");
            int id = res.getIdentifier("group_" + g, "string", context.getPackageName());

            XUiGroup group = new XUiGroup();
            group.name = name;
            group.title = (id > 0 ? res.getString(id) : name);
            uiGroups.add(group);
        }

        return uiGroups;
    }
}
