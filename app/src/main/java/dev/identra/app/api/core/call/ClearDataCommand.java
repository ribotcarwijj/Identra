package dev.identra.app.api.core.call;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;

import dev.identra.app.DebugUtil;
import dev.identra.app.XUtil;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.app.LuaSimplePacket;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.api.core.database.LuaAppManager;
import dev.identra.app.utilities.BundleUtil;

public class ClearDataCommand extends CallCommandHandler {
    private static final String TAG = "Identra.ClearDataCommand";

    @SuppressWarnings("unused")
    public ClearDataCommand() {
        name = "clearData";
        requiresPermissionCheck = true;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        int packetUid = commandData.getExtras().getInt("user");
        int callingUid = Binder.getCallingUid();
        if(DebugUtil.isDebug())
            Log.d(TAG, "[ClearDataCommand] Command Handler executed! " +
                    "Packet UID=" + packetUid +
                    "\nAppId=" + XUtil.getAppId(packetUid) +
                    "\nUserId=" + XUtil.getUserId(packetUid) +
                    "\nUserUid=" + XUtil.getUserUid(packetUid, XUtil.getAppId(packetUid)) +
                    "\nCallingUid=" + callingUid +
                    "\nCallingUid AppId=" + XUtil.getAppId(callingUid) +
                    "\nCallingUid UserId=" + XUtil.getUserId(callingUid) +
                    "\nCallingUid UserUid=" + XUtil.getUserUid(callingUid, XUtil.getAppId(callingUid)));

        return BundleUtil.createResultStatus(
                LuaAppManager.clearData(
                        packetUid,
                        commandData.getDatabase()));
    }

    public static Bundle invoke(Context context, LuaSimplePacket packet) {
        return XProxyContent.luaCall(
                context,
                "clearData",
                packet.toBundle());
    }
}
