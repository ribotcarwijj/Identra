package dev.identra.app.api.xmock.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.XResult;
import dev.identra.app.api.settings.LuaSettingPacket;
import dev.identra.app.api.xmock.database.LuaSettingsManager;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.logger.XLog;

public class PutMockSettingCommand extends CallCommandHandler {
    public PutMockSettingCommand() {
        name = "putMockSetting";
        requiresPermissionCheck = true;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        LuaSettingPacket packet = commandData.readExtrasAs(LuaSettingPacket.class);
        if(packet == null) return XResult.create().setMethodName("putMockSetting").setFailed("Setting Packet is NULL").toBundle();
        packet.resolveUserID();
        packet.ensureCode(LuaSettingPacket.CODE_INSERT_UPDATE_SETTING);

        XLog.i("[" + name + "] Command: packet=\n" + packet);

        XResult res1 = null;
        XResult res2 = null;
        switch (packet.getCode()) {
            case LuaSettingPacket.CODE_INSERT_UPDATE_SETTING:
            case LuaSettingPacket.CODE_DELETE_SETTING:
                return LuaSettingsManager.putSetting(commandData.getContext(), commandData.getDatabase(), packet).toBundle();
            case LuaSettingPacket.CODE_INSERT_UPDATE_DEFAULT_SETTING:
            case LuaSettingPacket.CODE_DELETE_DEFAULT_SETTING:
                return LuaSettingsManager.putDefaultMappedSetting(commandData.getContext(), commandData.getDatabase(), packet).toBundle();
            case LuaSettingPacket.CODE_INSERT_UPDATE_DEFAULT_AND_SETTING:
            case LuaSettingPacket.CODE_DELETE_DEFAULT_AND_SETTING:
                res1 = LuaSettingsManager.putSetting(commandData.getContext(), commandData.getDatabase(), packet);
                res2 = LuaSettingsManager.putDefaultMappedSetting(commandData.getContext(), commandData.getDatabase(), packet);
                break;
            default: return XResult.create().setMethodName("putMockSetting").setFailed("Unsupported Code: " + packet.getCode()).toBundle();
        } return XResult.combine(res1, res2).toBundle();
    }

    public static Bundle invoke(Context context, LuaSettingPacket packet) {
        return XProxyContent.luaCall(
                context,
                "putMockSetting",
                packet.toBundle());
    }
}
