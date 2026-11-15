package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.utilities.BundleUtil;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.configs.AppProfileUtils;
import dev.identra.app.x.core.configs.PathDetails;
import dev.identra.app.x.core.hook.AppProviderApi;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.identity.UserIdentityUtils;

public class GetAppDirectoriesCommand extends CallCommandHandlerEx {
    public GetAppDirectoriesCommand() {
        name = "getAppDirectories";
        requiresPermissionCheck = false;    //ToDO: Flip to true!
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        List<PathDetails> paths = AppProfileUtils.getAppDirectories(commandData.getUserId(), commandData.getCategory());
        Bundle b = new Bundle();
        b.putString("paths", PathDetails.encodeDetails(paths));
        return b;
    }

    public static List<PathDetails> get(Context context, UserIdentity identity) {
        return PathDetails.fromEncoded(
                BundleUtil.readString(XProxyContent.luaCall(context, "getAppDirectories", identity.toIdentityBundle()),
                        "paths", ""));
    }
}
