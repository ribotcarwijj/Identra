package dev.identra.app.x.core.commands.query;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;

import java.util.Collection;
import java.util.List;

import dev.identra.app.DebugUtil;
import dev.identra.app.XLegacyCore;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.api.hook.IdentraHookConversions;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.hook.AppProviderUtils;
import dev.identra.app.x.core.hook.AssignmentApi;
import dev.identra.app.x.core.hook.AssignmentPacket;
import dev.identra.app.x.core.commands.QueryCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.QueryPacket;
import dev.identra.app.x.core.database.sql.SQLDatabase;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.settings.data.SettingsApi;

/*
    Remove the Unused Constructors
 */
public class GetAssignedHooksExCommand extends QueryCommandHandlerEx {
    private static final String TAG = LibUtil.generateTag(GetAssignedHooksExCommand.class);

    public GetAssignedHooksExCommand() { this.name = "getExAssignedHooks"; this.requiresPermissionCheck = false; }

    public static Collection<XHook> get(Context context, boolean marshall, int uid, String packageName) {
        return IdentraHookConversions.fromCursor(
                XProxyContent.luaQuery(
                        context,
                        marshall ? "getExAssignedHooks2" : "getExAssignedHooks",
                        UserIdentity.createSnakeQueryUID(uid, packageName)), marshall, true);
    }

    @Override
    public Cursor handle(QueryPacket commandData) throws Throwable {
        MatrixCursor result = new MatrixCursor(new String[]{marshall ? "blob" : "json", "used"});

        SQLDatabase db = commandData.getDatabase();
        List<String> collections = db.executeWithWriteLock(() -> SettingsApi.getCollectionsValue(
                db,
                commandData.getUserId()));

        List<AssignmentPacket> assignments = db.executeWithReadLock(() -> AssignmentApi.getAssignments(
                db,
                commandData.getUserId(),
                commandData.getCategory()));

        if(DebugUtil.isDebug())
            Log.d(TAG, Str.fm("Got Assignments, UserId=%s UID=%s Category=%s Size=%s", commandData.getUserId(), commandData.getUid(), commandData.getCategory(), ListUtil.size(assignments)));

        for(AssignmentPacket assignment : AppProviderUtils.filterAssignments(assignments, false, false))
            XLegacyCore.writeHookFromCache(
                    result,
                    assignment.hook,
                    String.valueOf(assignment.used),
                    assignment.getCategory(),
                    collections,
                    marshall);

        return result;
    }


    /*
     @Override
    public Cursor handle(QueryPacket_old commandData) throws Throwable {
        String[] selection = commandData.getSelection();
        boolean all = (selection != null && selection.length == 1 && "all".equals(selection[0]));
        return CursorUtil.toMatrixCursor(
                UberCore888.getHooks(commandData.getContext(), commandData.getDatabase(), all),
                marshall,
                IdentraHook.FLAG_WITH_LUA);
    }

    public static Cursor invoke(Context context, boolean marshall) {
        return XProxyContent.luaQuery(
                context,
                marshall ? "getHooks2" : "getHooks");
    }

    public static Cursor invoke(Context context, boolean marshall, boolean all) {
        return XProxyContent.luaQuery(
                context,
                marshall ? "getHooks2" : "getHooks", new String[] { all ? "all" : "some" });
    }
     */
}
