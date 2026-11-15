package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.configs.AppProfile;
import dev.identra.app.x.core.configs.ProfileApi;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.DatabaseHelpEx;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.identity.UserIdentityIO;

public class GetProfileCommand extends CallCommandHandlerEx {
    private static final String TAG = "Identra.GetProfileCommand";

    public static final String COMMAND_NAME = "getProfile";

    public GetProfileCommand() {
        name = COMMAND_NAME;
        requiresPermissionCheck = true;
        this.requiresSingleThread = true;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        int userId = commandData.getUserId();
        String packageName = commandData.getCategory();
        String name = commandData.getExtraString(AppProfile.FIELD_NAME);
        if(DebugUtil.isDebug())
            Log.d(TAG, Str.fm("UserId=%s  Pkg=%s  Name=%s", userId, packageName, name));

        boolean prepRes = DatabaseHelpEx.prepareDatabase(commandData.getDatabase(), AppProfile.TABLE_INFO);
        if(!prepRes) {
            Log.e(TAG, "Failed to Prepare Table: " + Str.toStringOrNull(AppProfile.TABLE_INFO));
            return A_CODE.FAILED.toBundle();
        }

        //ToDO: Lock
        AppProfile profile = ProfileApi.get(commandData.getDatabase(), userId, packageName, name);
        if(DebugUtil.isDebug())
            Log.d(TAG, Str.fm("UserId=%s  Pkg=%s  Name=%s Profile==%s", userId, packageName, name, Str.toStringOrNull(profile)));

        return profile.toBundle();
    }

    public static AppProfile get(Context context, int uid, String packageName, String name) {
        Bundle dat = new Bundle();
        UserIdentity userIdentity = UserIdentity.fromUid(uid, packageName);
        UserIdentityIO.toBundleEx(userIdentity, dat);
        dat.putString(AppProfile.FIELD_NAME, name);
        if(DebugUtil.isDebug())
            Log.d(TAG, Str.fm("Calling [getProfile] uid=%s  Pkg=%s  Name=%s", uid, packageName, name));

        Bundle res = XProxyContent.luaCall(context, COMMAND_NAME, dat);
        if(DebugUtil.isDebug())
            Log.d(TAG, Str.fm("Called [getProfile] uid=%s  Pkg=%s  Name=%s Bundle=%s", uid, packageName, name, Str.toStringOrNull(res)));

        AppProfile profile = new AppProfile();
        profile.populateFromBundle(res);
        if(DebugUtil.isDebug())
            Log.d(TAG, Str.fm("Returning result [getProfile] uid=%s  Pkg=%s  Name=%s Result=%s", uid, packageName, name, Str.toStringOrNull(profile)));

        return profile;
    }
}