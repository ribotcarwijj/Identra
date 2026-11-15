package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import dev.identra.app.BuildConfig;
import dev.identra.app.DebugUtil;
import dev.identra.app.Identra;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.utilities.BundleUtil;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.database.DatabasePathUtil;
import dev.identra.app.x.core.hook.AppProviderApi;


public class GetBridgeVersionCommand extends CallCommandHandlerEx {
    public static final String TAG = LibUtil.generateTag(GetBridgeVersionCommand.class);

    public static final String FIELD_CURRENT = "current_version";
    public static final String DEFAULT = "null-bridge-error-ensure-reboot";

    public static final String COMMAND_NAME = "getBridgeVersion";
    private static String LAST_VERSION = null;

    public GetBridgeVersionCommand() {
        name = COMMAND_NAME;
        requiresPermissionCheck = true;
        requiresSingleThread = true;
    }

    public static void init() {
        if(Str.isEmpty(LAST_VERSION))
            LAST_VERSION = Str.createCopy(BuildConfig.VERSION_NAME);
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        if(Str.isEmpty(LAST_VERSION))
            LAST_VERSION = Str.createCopy(BuildConfig.VERSION_NAME);

        Bundle res = new Bundle();
        res.putString(FIELD_CURRENT, LAST_VERSION);
        return res;
    }

    public static String get(Context context) {
        String result = Str.ensureIsNotNullOrDefault(internalGet(context), DEFAULT);
        if(DebugUtil.isDebug())
            Log.d(TAG, "Bridge Version Check Result=" + result + " Current App Version: " + BuildConfig.VERSION_NAME);

        return result;
    }

    private static String internalGet(Context context) {
        Bundle result = XProxyContent.luaCall(context, COMMAND_NAME);
        if(result == null || !result.containsKey(FIELD_CURRENT)) return DEFAULT;
        return result.getString(FIELD_CURRENT, DEFAULT);
    }
}
