package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.hook.AppProviderApi;
import dev.identra.app.x.core.identity.UserIdentityIO;
import dev.identra.app.x.core.identity.UserIdentityUtils;

public class ClearAppDataCommand extends CallCommandHandlerEx {
    public ClearAppDataCommand() {
        name = "clearAppData";
        requiresPermissionCheck = true;
        requiresSingleThread = true;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        String packageName = commandData.getCategory();
        return A_CODE.result(AppProviderApi.clearAppData(commandData.getContext(), packageName)).toBundle();
    }


    public static A_CODE clear(Context context, String packageName) {
        Bundle b = new Bundle();
        b.putString(UserIdentityIO.FIELD_CATEGORY, packageName);
        Bundle res = XProxyContent.luaCall(context, "clearAppData", b);
        return A_CODE.fromBundle(res);
    }
}
