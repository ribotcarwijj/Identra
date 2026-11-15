package dev.identra.app.api.core.call;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.app.AppPacket;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.api.core.provider.IdentraAppProvider;

public class GetAppCommand extends CallCommandHandler {
    @SuppressWarnings("unused")
    public GetAppCommand() {
        name = "getApp";
        requiresPermissionCheck = true;
        requiresSingleThread = true;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        AppPacket packet = commandData.readExtrasAs(AppPacket.class);
        if(packet == null)
            throw new Exception("App Packet was NULL... [getApp]");

        return IdentraAppProvider.getApp(
                commandData.getContext(),
                commandData.getDatabase(),
                Binder.getCallingUid(),
                packet.getCategory(),
                packet.isInitForceStop(),
                packet.isInitSettings()).toBundle();
    }

    public static Bundle invoke(Context context, AppPacket packet) {
        return XProxyContent.luaCall(
                context,
                "getApp",
                packet.toBundle());
    }
}
