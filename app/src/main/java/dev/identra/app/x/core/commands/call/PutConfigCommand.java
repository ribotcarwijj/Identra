package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.x.Str;
import dev.identra.app.x.ui.core.UserClientAppContext;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.XPacket;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.configs.ConfigApi;
import dev.identra.app.x.core.configs.XPConfig;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.ActionFlag;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.settings.SettingHolder;
import dev.identra.app.x.core.settings.data.SettingPacket;
import dev.identra.app.x.core.settings.data.SettingsApi;

public class PutConfigCommand extends CallCommandHandlerEx {
    private static final String TAG = "Identra.PutConfigCommand";

    public static final String COMMAND_NAME = "putConfig"; //Intermap <Clazz, String> COMMNAND_CLASS, NAME, so it can be called from Packet as Clazz

    public PutConfigCommand() {
        name = COMMAND_NAME;
        requiresPermissionCheck = true;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        XPacket<XPConfig> packet = new XPacket<XPConfig>();
        packet.fromBundle(commandData.extras, XPConfig.class);

        if(DebugUtil.isDebug())
            Log.d(TAG, "Command Handler got Config Packet! Packet=" + Str.toStringOrNull(packet));

        return ConfigApi.single_locked(commandData.getDatabase(), packet).toBundle();
    }
}
