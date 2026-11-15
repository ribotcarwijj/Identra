package dev.identra.app.api.core.query;

import android.content.Context;
import android.database.Cursor;

import dev.identra.app.XLegacyCore;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.xstandard.QueryCommandHandler;
import dev.identra.app.api.xstandard.command.QueryPacket_old;
import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.utilities.CursorUtil;

public class GetHooksCommand extends QueryCommandHandler {
    @SuppressWarnings("unused")
    public GetHooksCommand() { this(false); }
    public GetHooksCommand(boolean marshall) {
        this.marshall = marshall;
        this.name = marshall ? "getHooks2" : "getHooks";
        requiresPermissionCheck = false;
        requiresSingleThread = true;
    }

    @Override
    public Cursor handle(QueryPacket_old commandData) throws Throwable {
        String[] selection = commandData.getSelection();
        boolean all = (selection != null && selection.length == 1 && "all".equals(selection[0]));
        /*return CursorUtil.toMatrixCursor(
                XLegacyCore.getHooks(commandData.getContext(), commandData.getDatabase(), all),
                marshall,
                IdentraHook.FLAG_WITH_LUA);*/

        return null;
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
}
