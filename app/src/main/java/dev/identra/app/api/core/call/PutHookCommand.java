package dev.identra.app.api.core.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.XResult;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.api.hook.LuaHookPacket;
import dev.identra.app.api.core.provider.IdentraHookProvider;

public class PutHookCommand extends CallCommandHandler {
    public static PutHookCommand create() { return new PutHookCommand(); };
    public PutHookCommand() {
        name = "putHook";
        requiresPermissionCheck = true;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        LuaHookPacket packet = commandData.readExtrasAs(LuaHookPacket.class);
        if(packet == null) return XResult.fromInvalidPacket(name, LuaHookPacket.class).toBundle();
        return IdentraHookProvider.putHook(
                commandData.getContext(),
                commandData.getDatabase(),
                packet).toBundle();
    }

    public static Bundle invoke(Context context, LuaHookPacket packet) {
        return XProxyContent.luaCall(
                context,
                "putHook",
                packet.toBundle());
    }
}
