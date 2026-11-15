package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;

import java.util.List;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.utilities.BundleUtil;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.configs.AppProfileUtils;
import dev.identra.app.x.core.configs.PathDetails;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.hook.AppProviderApi;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.identity.UserIdentityIO;
import dev.identra.app.x.core.identity.UserIdentityUtils;

public class ForceStopAppCommand extends CallCommandHandlerEx {
    public ForceStopAppCommand() {
        name = "forceToStop";
        requiresPermissionCheck = true;
        requiresSingleThread = true;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        int uid = commandData.getUid();
        String packageName = commandData.getCategory();
        return A_CODE.result(AppProviderApi.forceStop(commandData.getContext(), UserIdentityUtils.getUserId(uid), packageName)).toBundle();
    }


    public static A_CODE stop(Context context, int uid, String packageName) {
        Bundle b = new Bundle();
        b.putInt(UserIdentityIO.FIELD_UID, uid);
        b.putString(UserIdentityIO.FIELD_CATEGORY, packageName);
        Bundle res = XProxyContent.luaCall(context, "forceToStop", b);
        return A_CODE.fromBundle(res);
    }
}
