package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.utilities.BundleUtil;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.hook.AppProviderApi;

public class GetBridgeVersion extends CallCommandHandlerEx {
    public static int BRIDGE_VERSION = 150; //Since this is Static in the Context of the Service it will not Update until User Reboot ?
                                            //Do more cache ing with this ?
    public GetBridgeVersion() {
        name = "getBridgeVersion";
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        int res = AppProviderApi.getVersion(commandData.getContext());
        return AppProviderApi.INVALID_VERSION == res ?
                null :
                BundleUtil.createSingleInt("version", BRIDGE_VERSION);
    }

    public static boolean isUpdated(Context context) {
        return get(context) == BRIDGE_VERSION;  //Compare User Context App to Service Side Context
    }

    public static int get(Context context) {
        return BundleUtil.readInteger(XProxyContent.luaCall(
                context,
                "getBridgeVersion"), "version", 0);
    }
}
