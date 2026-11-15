package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.hook.AssignmentApi;
import dev.identra.app.x.core.hook.AssignmentsPacket;

/*
    ToDo: Make Legacy version
 */
public class AssignHooksCommand extends CallCommandHandlerEx {
    public AssignHooksCommand() {
        name = "assignExHooksEx";
        requiresPermissionCheck = true;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        return AssignmentApi.assignHooks(
                commandData.getContext(),
                commandData.getDatabase(),
                commandData.readExtraAs(AssignmentsPacket.class)).toBundle();
    }


    public static A_CODE call(Context context, AssignmentsPacket packet) {
        return A_CODE.fromBundle(XProxyContent.luaCall(
                context,
                "assignExHooksEx",
                packet.toBundle()));
    }
}