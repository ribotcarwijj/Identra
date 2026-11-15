package dev.identra.app.x.core.commands.query;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.xstandard.database.SqlQuerySnake;
import dev.identra.app.utilities.CursorUtil;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.data.utils.ListUtils;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.QueryCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.QueryPacket;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.database.sql.SQLSnake;
import dev.identra.app.x.core.hook.AssignmentPacket;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.settings.data.SettingPacket;
import dev.identra.app.x.core.settings.data.SettingsApi;

@SuppressWarnings("unused")
public class GetSettingsExCommand extends QueryCommandHandlerEx {
    private static final String TAG = LibUtil.generateTag(GetSettingsExCommand.class);

    public static final String COMMAND_NAME = "getExSettingsEx";

    public static final int FLAG_ONE = 0x0;
    public static final int FLAG_TWO = 0x2;

    public GetSettingsExCommand() { this.name = COMMAND_NAME; this.requiresPermissionCheck = false; this.requiresSingleThread = true; }

    @Override
    public Cursor handle(QueryPacket commandData) throws Throwable {
        try {
            if(commandData.isDump()) {
                return CursorUtil.toMatrixCursor_final(SettingsApi.dumpSettings(
                                commandData.getDatabase()),
                        marshall, 0);
            } else {
                return CursorUtil.toMatrixCursor_final(SettingsApi.getAllSettings(
                        commandData.getContext(),
                        commandData.getDatabase(),
                        commandData.getUserId(),
                        commandData.getCategory()), marshall, 0);
            }
        }catch (Exception e) {
            Log.e(TAG, "Get Settings Command failed! Error=" + e);
            return null;
        }
    }

    public static List<SettingPacket> dump(Context context, boolean marshall) {
        return ListUtil.copyToArrayList(
                CursorUtil.readCursorAs_final(
                        XProxyContent.luaQuery(context, XProxyContent.commandName(COMMAND_NAME, marshall), new String[] { ActionPacket.ACTION_DUMP }),
                        marshall, SettingPacket.class));
    }

    public static Map<String, String> getAsMap(Context context, boolean marshall, int uid, String category, int extraFlags) {
        return ListUtil.toMap(
                get(context, marshall, uid, category, extraFlags),
                SettingPacket.TO_MAP);
    }

    public static List<SettingPacket> get(Context context, boolean marshall, int uid, String category, int extraFlags) {
        return ListUtil.copyToArrayList(
                CursorUtil.readCursorAs_final(
                        XProxyContent.luaQuery(context, XProxyContent.commandName(COMMAND_NAME, marshall),
                                UserIdentity.createSnakeQueryUID(uid, category).whereColumn(ActionPacket.FIELD_CODE, extraFlags).asSnake()),
                        marshall, SettingPacket.class));
    }
}
