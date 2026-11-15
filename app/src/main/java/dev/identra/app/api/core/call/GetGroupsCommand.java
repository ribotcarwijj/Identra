package dev.identra.app.api.core.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.XLegacyCore;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.utilities.BundleUtil;

public class GetGroupsCommand extends CallCommandHandler {
    public static GetGroupsCommand create() { return new GetGroupsCommand(); }
    public GetGroupsCommand() {
        name = "getGroups";
        requiresPermissionCheck = false;
        this.requiresSingleThread = true;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        /*return BundleUtil.
                createFromStringList(
                        "groups",
                        XLegacyCore.getGroups(commandData.getContext(), commandData.getDatabase()));
    */
        return null;
    }

    public static Bundle invoke(Context context) {
        return XProxyContent.luaCall(
                context,
                "getGroups");
    }
}
