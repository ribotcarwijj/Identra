package dev.identra.app.api.xmock.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.XUtil;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.XResult;
import dev.identra.app.api.settings.LuaSettingPacket;
import dev.identra.app.api.core.provider.IdentraAppProvider;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;

public class ClearAppDataCommand extends CallCommandHandler {
    @SuppressWarnings("unused")
    public ClearAppDataCommand() {
        name = "clearAppData";
        requiresPermissionCheck = true;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        LuaSettingPacket packet = commandData.readExtrasAs(LuaSettingPacket.class);
        XResult res = XResult.create().setMethodName("killApp").setExtra(packet.toString());
        IdentraAppProvider.forceStop(
                commandData.getContext(),
                packet.getCategory(),
                XUtil.getUserId(packet.getUser()),
                res);

        IdentraAppProvider.clearAppData(commandData.getContext(), packet.getCategory());
        return XResult.create().setSucceeded("Finished").toBundle();
    }

    public static XResult invokeEx(Context context, String packageName, int uid) {
        LuaSettingPacket packet = new LuaSettingPacket();
        packet.setCategory(packageName);
        packet.setUser(uid);
        return XResult.from(invoke(context, packet));
    }

    public static Bundle invoke(Context context, LuaSettingPacket packet) {
        return XProxyContent.mockCall(
                context,
                "clearAppData",
                packet.toBundle());
    }
}
