package dev.identra.app.api.core.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.api.core.provider.IdentraAppProvider;
import dev.identra.app.utilities.BundleUtil;

public class GetVersionCommand extends CallCommandHandler {
    public static GetVersionCommand create() { return new GetVersionCommand(); };
    public GetVersionCommand() {
        name = "getVersion";
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        int res = IdentraAppProvider.getVersion(commandData.getContext());
        if(res == -55) return null;
        return BundleUtil.createSingleInt("version", res);
    }

    public static Bundle invoke(Context context) {
        return XProxyContent.luaCall(
                context,
                "getVersion");
    }
}
