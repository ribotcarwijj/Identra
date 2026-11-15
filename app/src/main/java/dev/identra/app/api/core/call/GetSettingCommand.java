package dev.identra.app.api.core.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.settings.LuaSettingPacket;
import dev.identra.app.api.xmock.database.LuaSettingsManager;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.utilities.BundleUtil;


public class GetSettingCommand extends CallCommandHandler {

    public static GetSettingCommand create() { return new GetSettingCommand(); };
    public GetSettingCommand() {
        name = "getSetting";
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        LuaSettingPacket packet = commandData.readExtrasAs(LuaSettingPacket.class);
        packet.resolveUserID();

        if(packet.isGetObject()) {
            return LuaSettingsManager.getSetting(
                    commandData.getDatabase(),
                    packet.getName(),
                    packet.getUser(),
                    packet.getCategory()).toBundle();
        }else {
            return BundleUtil.createSingleString("value",
                    LuaSettingsManager.getSettingValue(
                            commandData.getContext(),
                            commandData.getDatabase(),
                            packet));
        }
    }

    public static Bundle invoke(Context context, Bundle b) {
        return XProxyContent.luaCall(
            context,
            "getSetting", b);
    }

    public static Bundle invoke(Context context, LuaSettingPacket packet) {
        return XProxyContent.luaCall(
                        context,
                        "getSetting",
                        packet.toBundle());
    }
}
