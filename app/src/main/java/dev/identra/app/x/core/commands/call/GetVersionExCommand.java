package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.BuildConfig;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.core.call.GetVersionCommand;
import dev.identra.app.api.core.database.LuaAppManager;
import dev.identra.app.api.core.provider.IdentraAppProvider;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.utilities.BundleUtil;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.hook.AppProviderApi;

/**
 * ToDo: Make use of this, using this can tell if the Bridge has been Updated aka Device ReBoot
 */
public class GetVersionExCommand extends CallCommandHandlerEx {
    public static final String COMMAND_NAME = "getVersion";
    public GetVersionExCommand() {
        name = COMMAND_NAME;
        requiresPermissionCheck = true;
        requiresSingleThread = true;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        int res = AppProviderApi.getVersion(commandData.getContext());
        return AppProviderApi.INVALID_VERSION == res ?
                null :
                BundleUtil.createSingleInt("version", res);
    }

    public static Bundle invoke(Context context) {
        return XProxyContent.luaCall(
                context,
                "getVersion");
    }

    public static int get(Context context) {
        return BundleUtil.readInteger(XProxyContent.luaCall(
                context,
                "getVersion"), "version", AppProviderApi.INVALID_VERSION);
    }
}
