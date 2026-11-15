package dev.identra.app.api.xmock.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.XResult;
import dev.identra.app.api.xmock.database.MockConfigManager;
import dev.identra.app.api.configs.MockConfigPacket;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;

public class PutMockConfigCommand extends CallCommandHandler {
    public static PutMockConfigCommand create() { return new PutMockConfigCommand(); };
    public PutMockConfigCommand() {
        name = "putMockConfig";
        requiresPermissionCheck = true;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        MockConfigPacket packet = commandData.readExtrasAs(MockConfigPacket.class);
        if(packet == null) return XResult.create().setMethodName("putMockConfig").setFailed("Mock Config Packet is NULL!").toBundle();
        packet.ensureCode(MockConfigPacket.CODE_INSERT_UPDATE_CONFIG);
        switch (packet.getCode()) {
            case MockConfigPacket.CODE_DELETE_CONFIG:
            case MockConfigPacket.CODE_INSERT_UPDATE_CONFIG:
                return MockConfigManager.putMockConfig(commandData.getContext(), commandData.getDatabase(), packet).toBundle();
        }

        return XResult.create().setMethodName("putMockConfig").setFailed("Failed to find Command Handler for Mock Config Packet! Code=" + packet.getCode()).toBundle();
    }

    public static Bundle invoke(Context context, MockConfigPacket packet) {
        return XProxyContent.mockCall(
                context,
                "putMockConfig",
                packet.toBundle());
    }
}
