package dev.identra.app.x.core.commands.query;

import android.content.Context;
import android.database.Cursor;

import java.util.List;

import dev.identra.app.XLegacyCore;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.hook.IdentraHookConversions;
import dev.identra.app.utilities.CursorUtil;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.ui.adapters.hooks.elements.XHookIO;
import dev.identra.app.x.core.commands.QueryCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.QueryPacket;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.database.DatabaseHelpEx;

public class GetHooksCommand extends QueryCommandHandlerEx {
    public static final String COMMAND_NAME = "getHooks";

    public GetHooksCommand() { this.name = COMMAND_NAME; this.requiresSingleThread = true; this.requiresPermissionCheck = false; }

    @Override
    public Cursor handle(QueryPacket commandData) throws Throwable {
        if(commandData.isDump()) {
            return CursorUtil.toMatrixCursor(
                    DatabaseHelpEx.getFromDatabase(commandData.getDatabase(),
                            XHook.TABLE_NAME,
                            XHook.class,
                            true),
                    marshall,
                    XHookIO.FLAG_WITH_LUA);
        } else {
            return CursorUtil.toMatrixCursor(
                    XLegacyCore.getHooks(XLegacyCore.getCollections(commandData.getDatabase()), commandData.isAll()),
                    marshall,
                    XHookIO.FLAG_WITH_LUA);
        }
    }

    public static List<XHook> dump(Context context, boolean marshall) {
        return ListUtil.copyToArrayList(
                IdentraHookConversions.fromCursor(
                        XProxyContent.luaQuery(context, XProxyContent.commandName(COMMAND_NAME, marshall), new String[] { ActionPacket.ACTION_DUMP }),
                        marshall, true));
    }

    public static List<XHook> getHooks(Context context, boolean marshall) { return ListUtil.copyToArrayList(IdentraHookConversions.fromCursor(invoke(context, marshall, false), marshall, true)); }
    public static List<XHook> getHooks(Context context, boolean marshall, boolean all) { return ListUtil.copyToArrayList(IdentraHookConversions.fromCursor(invoke(context, marshall, all), marshall, true)); }
    public static Cursor invoke(Context context, boolean marshall, boolean all) {
        return XProxyContent.luaQuery(
                context,
                XProxyContent.commandName(COMMAND_NAME, marshall), all ? new String[] { ActionPacket.ACTION_ALL } : null);
    }
}
