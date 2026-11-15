package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;

import java.util.List;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.utilities.BundleUtil;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.hook.AppProviderApi;

public class GetCollectionsCommand extends CallCommandHandlerEx {
    public GetCollectionsCommand() {
        name = "getAllCollections";
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        int res = AppProviderApi.getVersion(commandData.getContext());
        return AppProviderApi.INVALID_VERSION == res ?
                null :
                BundleUtil.createSingleInt("version", res);
    }

    public static Bundle invoke(Context context) {
        return XProxyContent.luaCall(
                context,
                "getAllCollections");
    }

    public List<String> getAllCollections(Context context) {
        //
        return null;
    }
}
