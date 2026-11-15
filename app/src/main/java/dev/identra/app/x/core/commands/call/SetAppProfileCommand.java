package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.x.Str;
import dev.identra.app.x.file.FileUtils;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.configs.AppProfile;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.DatabaseHelpEx;

public class SetAppProfileCommand extends CallCommandHandlerEx {
    private static final String TAG = "Identra.SetAppProfileCommand";
    public static final String COMMAND_NAME = "setAppProfile";

    public SetAppProfileCommand() {
        name = COMMAND_NAME;
        requiresPermissionCheck = true;
    }

    /*
        They click Create then they set extra flag or something Apply or not

     */

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        AppProfile profile = commandData.readExtraAs(AppProfile.class);
        if(DebugUtil.isDebug())
            Log.d(TAG, "Received a Set App Profile Command: Packet=" + Str.toStringOrNull(profile));

        //Set the setting here as well "
        //No matter whatever is sent it should be valid
        //It can exist if so just push over old thats all
        //If not exist then push in general
        //Extra flag to "apply" it


        //FileUtils.readFileContentsAsString()

        //return code.toBundle();
        return A_CODE.FAILED.toBundle();
    }

    public static A_CODE call(Context context, AppProfile profile) {
        if(DebugUtil.isDebug())
            Log.d(TAG, "Calling [putAppProfile] Command, Profile=" + Str.toStringOrNull(profile));

        Bundle b = profile.toBundle();
        if(DebugUtil.isDebug())
            Log.d(TAG, "Calling [putAppProfile] Command, Profile Bundle=" + Str.toStringOrNull(b));


        return A_CODE.fromBundle(XProxyContent.luaCall(
                context,
                COMMAND_NAME, b));
    }
}
