package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import dev.identra.app.XLegacyCore;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.utilities.BundleUtil;
import dev.identra.app.utilities.JSONUtil;
import dev.identra.app.x.Str;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.commands.query.GetHooksCommand;

public class GetHookCommand extends CallCommandHandlerEx {

    public static final String FIELD = "hook";
    public static final String COMMAND_NAME = "getHook";

    public GetHookCommand() {
        name = COMMAND_NAME;
        requiresPermissionCheck = true;
        this.requiresSingleThread = true;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        return BundleUtil.createSingleString(
                FIELD,
                XLegacyCore.getHookAsJsonString(commandData.getExtraString(FIELD)));
    }

    public static XHook getEx(Context context, String hookId) {
        if(Str.isEmpty(hookId))
            return null;

        XHook hook = XHook.create();
        Bundle res = XProxyContent.luaCall(context, COMMAND_NAME, BundleUtil.createSingleString(FIELD, hookId));
        if(res == null)
            return null;

        String jsonData = res.getString(FIELD);
        if(Str.isEmpty(jsonData))
            return null;

        hook.fromJSONString(jsonData);
        return hook;
    }
}