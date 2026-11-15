package dev.identra.app.x.core.commands.query;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;

import java.util.Collection;
import java.util.List;

import dev.identra.app.DebugUtil;
import dev.identra.app.XLegacyCore;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.hook.AssignmentApi;
import dev.identra.app.x.core.hook.AssignmentPacket;
import dev.identra.app.x.core.commands.QueryCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.QueryPacket;
import dev.identra.app.x.core.database.sql.SQLDatabase;
import dev.identra.app.x.core.settings.data.SettingsApi;

public class GetAssignedHooksLegacyCommand extends QueryCommandHandlerEx {
    private static final String TAG = LibUtil.generateTag(GetAssignedHooksLegacyCommand.class);

    public GetAssignedHooksLegacyCommand() { this.name = "getAssignedHooks"; this.requiresPermissionCheck = true; }

    @Override
    public Cursor handle(QueryPacket commandData) throws Throwable {
        MatrixCursor result = new MatrixCursor(new String[]{marshall ? "blob" : "json", "used"});
        if(DebugUtil.isDebug())
            Log.d(TAG, "Get Assignments Legacy Command Invoked!");

        /*Make this more legacy*/
        SQLDatabase db = commandData.getDatabase();
        List<String> collection = db.executeWithWriteLock(() -> SettingsApi.getCollectionsValue(db, commandData.getUserId(true)));
        Collection<AssignmentPacket> assignments = db.executeWithReadLock(() -> AssignmentApi.getAssignments(db, commandData.getUserId(true), commandData.getCategory(true)));
        if (DebugUtil.isDebug())
            Log.d(TAG, Str.fm("Got Assigned Hooks Legacy, Count=%s Collection Count=%s UserInfo=%s", ListUtil.size(assignments), Str.joinList(collection), Str.noNL(Str.toStringOrNull(commandData.getUserIdentification(true)))));

        for (AssignmentPacket assignment : assignments)
            XLegacyCore.writeHookFromCache(
                    result,
                    assignment.hook,
                    String.valueOf(assignment.used),
                    assignment.getCategory(),
                    collection,
                    marshall);

        return result;
    }
}