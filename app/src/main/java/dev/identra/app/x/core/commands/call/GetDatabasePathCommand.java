package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.utilities.BundleUtil;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;

public class GetDatabasePathCommand extends CallCommandHandlerEx {
    public static final String FIELD_GROUP = "path";

    private static final String TAG = LibUtil.generateTag(GetDatabasePathCommand.class);

    public GetDatabasePathCommand() {
        name = "getDatabasePath";
        requiresPermissionCheck = true;
        this.requiresSingleThread = true;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        String path = commandData.getDatabase().getDirectory();
        if(DebugUtil.isDebug())
            Log.d(TAG, " Get Database path Command= " + path);

        return BundleUtil.createSingleString(FIELD_GROUP, path);
    }

    public static String get(Context context) {
        return BundleUtil
                .readString(
                        XProxyContent.luaCall(context, "getDatabasePath"), FIELD_GROUP);
    }
}