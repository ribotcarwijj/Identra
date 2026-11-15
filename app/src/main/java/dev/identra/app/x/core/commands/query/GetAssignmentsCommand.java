package dev.identra.app.x.core.commands.query;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.util.List;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.utilities.CursorUtil;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.runtime.RuntimeUtils;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.QueryCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.QueryPacket;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.database.sql.SQLSnake;
import dev.identra.app.x.core.hook.AppProviderUtils;
import dev.identra.app.x.core.hook.AssignmentApi;
import dev.identra.app.x.core.hook.AssignmentPacket;
import dev.identra.app.x.core.identity.UserIdentity;

public class GetAssignmentsCommand extends QueryCommandHandlerEx {
    private static final String TAG = LibUtil.generateTag(GetAssignmentsCommand.class);

    public static final String COMMAND_NAME = "getAssignments";

    public GetAssignmentsCommand() { this.name = COMMAND_NAME; this.requiresPermissionCheck = false; }

    @Override
    public Cursor handle(QueryPacket commandData) throws Throwable {
        if(commandData.isDump()) {
            return CursorUtil.toMatrixCursor_final(
                    AssignmentApi.dumpAssignments(commandData.getDatabase()),
                    marshall,
                    0);
        } else {
            return CursorUtil.toMatrixCursor_final(
                    AppProviderUtils.filterAssignments(
                            AssignmentApi.getAssignments(
                                    commandData.getDatabase(),
                                    commandData.getUserId(),
                                    commandData.getCategory()),
                            false,
                            commandData.getIntSelectionAt(2, 0) == 3),
                    marshall, 0);
        }
    }


    public static List<AssignmentPacket> dump(Context context, boolean marshall) {
        return ListUtil.copyToArrayList(
                CursorUtil.readCursorAs_final(
                        XProxyContent.luaQuery(context, XProxyContent.commandName(COMMAND_NAME, marshall), new String[] { ActionPacket.ACTION_DUMP }),
                        marshall, AssignmentPacket.class));
    }

    public static List<AssignmentPacket> get(Context context, boolean marshall, int uid, String category) { return get(context, marshall, uid, category, 0); }
    public static List<AssignmentPacket> get(Context context, boolean marshall, int uid, String category, int extraFlags) {
        return ListUtil.copyToArrayList(
                CursorUtil.readCursorAs_final(
                        XProxyContent.luaQuery(context, XProxyContent.commandName(COMMAND_NAME, marshall),
                                UserIdentity.createSnakeQueryUID(uid, category).whereColumn(ActionPacket.FIELD_CODE, extraFlags).asSnake()),
                        marshall, AssignmentPacket.class));
    }
}
