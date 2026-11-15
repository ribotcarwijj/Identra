package dev.identra.app.api.core.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.XResult;
import dev.identra.app.api.core.database.LuaAppManager;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;

public class ClearSettingsCommand extends CallCommandHandler {
    @SuppressWarnings("unused")
    public ClearSettingsCommand() {
        name = "clearSettings";
        requiresPermissionCheck = true;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable { return LuaAppManager.clearSettings(commandData.getExtras().getString("packageName"), commandData.getDatabase()).toBundle(); }
    public static XResult invoke(Context context, String packageName) {
        Bundle b = new Bundle();
        b.putString("packageName", packageName);
        return XResult.from(XProxyContent.luaCall(
                context,
                "clearSettings",
                b));
    }
}
