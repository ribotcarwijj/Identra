package dev.identra.app.api.core.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.XResult;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.api.core.database.LuaAppManager;
import dev.identra.app.api.app.LuaSimplePacket;

public class ClearAppCommand extends CallCommandHandler {
    public static ClearAppCommand create() { return new ClearAppCommand(); };
    public ClearAppCommand() {
        name = "clearApp";
        requiresPermissionCheck = true;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        LuaSimplePacket packet = commandData.readExtrasAs(LuaSimplePacket.class);
        if(packet == null) return XResult.fromInvalidPacket(name, LuaSimplePacket.class).toBundle();
        return XResult.create()
                .setMethodName("clearApp")
                .setExtra(packet.toString())
                .setResult(LuaAppManager.clearApp(
                        commandData.getContext(), commandData.getDatabase(), packet)).toBundle();
    }

    public static Bundle invoke(Context context, LuaSimplePacket packet) {
        return XProxyContent.luaCall(
                context,
                "clearApp",
                packet.toBundle());
    }
}
