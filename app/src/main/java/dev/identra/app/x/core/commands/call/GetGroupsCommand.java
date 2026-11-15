package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;

import java.util.List;

import dev.identra.app.XLegacyCore;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.utilities.BundleUtil;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;

public class GetGroupsCommand extends CallCommandHandlerEx {
    public static final String FIELD_GROUP = "groups";
    public static final String COMMAND_NAME = "getGroups";
    public GetGroupsCommand() {
        name = COMMAND_NAME;
        requiresPermissionCheck = true;
        this.requiresSingleThread = true;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        return BundleUtil.
                createFromStringList(
                        FIELD_GROUP,
                        XLegacyCore.getGroups(XLegacyCore.getCollections(commandData.getDatabase())));
    }

    public static List<String> get(Context context) {
        return BundleUtil
                .getStringArrayList(
                        XProxyContent.luaCall(context, COMMAND_NAME), FIELD_GROUP);
    }
}