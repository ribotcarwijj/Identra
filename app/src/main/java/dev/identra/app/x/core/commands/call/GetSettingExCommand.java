package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.database.ActionFlag;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.identity.UserIdentityIO;
import dev.identra.app.x.core.settings.data.SettingPacket;
import dev.identra.app.x.core.settings.data.SettingsApi;
import dev.identra.app.x.core.settings.data.SettingsApiUtils;

public class GetSettingExCommand extends CallCommandHandlerEx {
    private static final String TAG = "Identra.GetSettingExCommand";

    public static final String SETTING_USE_DEFAULT = "useDefault";
    public static final String SETTING_COLLECTION = "collection";
    public static final String SETTING_FORCE_STOP = "forcestop";
    public static final String SETTING_THEME = "theme";
    public static final String SETTING_SHOW = "show";

    public static final String SETTING_RESTRICT_NEW_APPS = "restrict_new_apps";
    public static final String SETTING_NOTIFY_NEW_APPS = "notify_new_apps";

    public static final String SETTING_VERBOSE_DEBUG = "verbose_debug_logs";
    public static final String SETTING_SELECTED_CONFIG = "selected_config";

    public static final String SETTING_COLLECTION_DEFAULT = dev.identra.IdentraNs.COLLECTION;
    public static final String SETTING_THEME_DEFAULT = "dark";


    //this.kill = GetSettingExCommand.getBool(context, GetSettingExCommand.SETTING_FORCE_STOP, uid, packageName); }

    public GetSettingExCommand() {
        name = "getExSettingEx";
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        return SettingsApi.getSettingOrBuiltInSetting(
                commandData.getDatabase(),
                        commandData.getUserId(),
                        commandData.getCategory(),
                        commandData.getExtraString(SettingPacket.FIELD_NAME)).toBundle();
    }

    public static String getConfig(Context context, int uid, String pkg) {
        if(!Str.isEmpty(pkg) && !UserIdentity.GLOBAL_NAMESPACE.equalsIgnoreCase(pkg)) {
            SettingPacket packet = get(context, SETTING_SELECTED_CONFIG, uid, pkg);
            return packet == null ? null : packet.value;
        }

        return null;
    }

    public static boolean getVerboseLogs(Context context) { return getBool(context, SETTING_VERBOSE_DEBUG,  Process.myUid(), SettingPacket.GLOBAL_CATEGORY); }
    public static boolean getVerboseLogs(Context context, int uid) { return getBool(context, SETTING_VERBOSE_DEBUG, uid, SettingPacket.GLOBAL_CATEGORY); }

    public static boolean restrictNewApps(Context context) { return getBool(context, SETTING_RESTRICT_NEW_APPS,  Process.myUid(), SettingPacket.GLOBAL_CATEGORY); }
    public static boolean restrictNewApps(Context context, int uid) { return getBool(context, SETTING_RESTRICT_NEW_APPS, uid, SettingPacket.GLOBAL_CATEGORY); }

    public static boolean notifyOnNewApps(Context context) { return getBool(context, SETTING_NOTIFY_NEW_APPS, Process.myUid(), SettingPacket.GLOBAL_CATEGORY); }
    public static boolean notifyOnNewApps(Context context, int uid) { return getBool(context, SETTING_NOTIFY_NEW_APPS, uid, SettingPacket.GLOBAL_CATEGORY); }

    public static String getShow(Context context, int uid) { return getGlobalValue(context, SETTING_SHOW, uid); }

    public static List<String> getCollections(Context context, int uid) { return Str.splitToList(get(context, SETTING_COLLECTION, uid, SettingPacket.GLOBAL_CATEGORY).value); }

    public static String getTheme(Context context, int uid) { return SettingsApiUtils.ensureIsTheme(get(context, SETTING_THEME, uid, SettingPacket.GLOBAL_CATEGORY).value); }

    public static boolean getBool(Context context, String settingName, int uid, String packageName) { return Str.toBool(get(context, settingName, uid, packageName).value); }

    public static String getGlobalValue(Context context, String settingName, int uid) { return getGlobal(context, settingName, uid).value; }
    public static SettingPacket getGlobal(Context context, String settingName, int uid) { return get(context, settingName, uid, SettingPacket.GLOBAL_CATEGORY); }

    public static SettingPacket get(Context context, String settingName, int uid, String packageName) { return get(context, settingName, UserIdentity.fromUid(uid, packageName)); }
    public static SettingPacket get(Context context, String settingName, UserIdentity userIdentity) {
        if(DebugUtil.isDebug())
            Log.d(TAG, "Getting Setting, Setting Name=" + settingName + " User ID=" + userIdentity.hasUid());

        return new SettingPacket(
                settingName,
                null,
                ActionPacket.create(ActionFlag.GET, false), userIdentity).sendCallRequest(context, "getExSettingEx");
    }
}