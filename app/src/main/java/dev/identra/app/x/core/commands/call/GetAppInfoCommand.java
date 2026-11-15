package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.app.AppPacket;
import dev.identra.app.api.core.provider.IdentraAppProvider;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.hook.AppProviderApi;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.core.identity.UserIdentity;

@SuppressWarnings("all")
public class GetAppInfoCommand extends CallCommandHandlerEx {
    private static final String TAG = "Identra.GetAppInfoCommand";

    public GetAppInfoCommand() {
        name = "getExAppEx";
        requiresPermissionCheck = true;
        requiresSingleThread = true;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        if(DebugUtil.isDebug())
            Log.d(TAG, "Called Get App Info! Extras=" + Str.toStringOrNull(commandData.extras));

        return AppProviderApi.getApp(
                commandData.getContext(),
                commandData.getDatabase(),
                commandData.getUid(),
                commandData.getCategory(),
                true,
                true).toBundle();
    }

    public static AppXpPacket get(Context context, int uid, String packageName) {
        return new AppXpPacket(
                XProxyContent.luaCall(
                        context,
                        "getExAppEx",
                        UserIdentity.fromUid(uid, packageName).toIdentityBundle()));
    }
}
