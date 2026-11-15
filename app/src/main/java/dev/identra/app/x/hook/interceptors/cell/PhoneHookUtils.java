package dev.identra.app.x.hook.interceptors.cell;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Process;
import android.util.Log;

import dev.identra.app.DebugUtil;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.call.GetSettingExCommand;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.identity.UserIdentityUtils;
import dev.identra.app.x.core.settings.data.SettingPacket;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class PhoneHookUtils {
    private static final String TAG = LibUtil.generateTag(PhoneHookUtils.class);

    public static final String SETTING_IMEI = RandomizersCache.SETTING_UNIQUE_IMEI;
    public static final String SETTING_SIM_COUNT = "cell.active.subscription.count";

    public static boolean canUseGlobal() { return Binder.getCallingUid() > 1001; }
    public static String getPackageNameFromUid(Context context, int uid) {
        try {
            PackageManager packageManager = context.getPackageManager();
            // Get the package info for the given UID
            String[] packageNames = packageManager.getPackagesForUid(uid);
            if (packageNames != null && packageNames.length > 0) {
                return packageNames[0]; // Return the first package name associated with the UID
            }
        } catch (SecurityException ignored) { }
        return null;
    }

    public static boolean isPhoneService(String name) { return "com.android.phone".equalsIgnoreCase(name); }

    public static String tryGetArgAsString(Object[] args, int index, String defaultValue) {
        try {
            Object v = args[index];
            if(v == null) return defaultValue;
            return (String)v;
        }catch (Exception ignored) { return defaultValue; }
    }

    public static int getActiveSimCount(Context context, int defaultCount) {
        return Str.tryParseInt(
                getSettingValue(context, SETTING_SIM_COUNT,
                        getPackageNameFromUid(context, Binder.getCallingUid())),
                canUseGlobal() ?
                Str.tryParseInt(getSettingValue(context, SETTING_SIM_COUNT, "global"), defaultCount) : defaultCount);
    }

    public static String getSettingValue(Context context, String settingName, int index, String callingPackage) { return getSettingValue(context, settingName + "." + index, index, callingPackage); }
    public static String getSettingValue(Context context, String settingName, String callingPackage) {
        try {
            int userId = UserIdentityUtils.getUserId(Process.myUid());
            if(DebugUtil.isDebug()) Log.d(TAG, "Getting Setting: " + settingName + " For Package: " + callingPackage + " UserId=" + userId);
            SettingPacket res = GetSettingExCommand.get(context, settingName, UserIdentity.from(userId, 0, callingPackage));
            return res.value;
        }catch (Exception e) {
            Log.e(TAG, "Failed to Get Setting: " + settingName + " Calling package=" + callingPackage);
            return null;
        }
    }
}
