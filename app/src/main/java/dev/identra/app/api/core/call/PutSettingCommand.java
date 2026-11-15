package dev.identra.app.api.core.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.settings.LuaSettingPacket;
import dev.identra.app.api.xmock.database.LuaSettingsManager;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;

public class PutSettingCommand extends CallCommandHandler {
    public static PutSettingCommand create() { return new PutSettingCommand(); };
    public PutSettingCommand() {
        name = "putSetting";
        requiresPermissionCheck = true;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        LuaSettingPacket packet = commandData.readExtrasAs(LuaSettingPacket.class);
        packet.resolveUserID();
        return LuaSettingsManager.putSetting(
                commandData.getContext(),
                commandData.getDatabase(),
                packet).toBundle();
    }

    public static Bundle invoke(Context context, LuaSettingPacket packet) {
        return XProxyContent.luaCall(
                context,
                "putSetting",
                packet.toBundle());
    }
}
