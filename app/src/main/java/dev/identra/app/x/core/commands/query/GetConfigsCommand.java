package dev.identra.app.x.core.commands.query;

import android.content.Context;
import android.database.Cursor;
import android.os.Process;

import java.util.List;
import java.util.Map;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.utilities.CursorUtil;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.core.commands.QueryCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.QueryPacket;
import dev.identra.app.x.core.configs.ConfigApi;
import dev.identra.app.x.core.configs.XPConfig;
import dev.identra.app.x.core.database.sql.SQLSnake;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.settings.data.SettingPacket;
import dev.identra.app.x.core.settings.data.SettingsApi;

public class GetConfigsCommand extends QueryCommandHandlerEx {

    public GetConfigsCommand() { this.name = "getConfigs"; this.requiresPermissionCheck = true; this.requiresSingleThread = true; }

    @Override
    public Cursor handle(QueryPacket commandData) throws Throwable {
        return CursorUtil.toMatrixCursor_final(ConfigApi.getConfigs(
                commandData.getDatabase(),
                commandData.getUserId()), marshall, 0);
    }

    public static List<XPConfig> get(Context context) { return get(context, false, Process.myUid()); }
    public static List<XPConfig> get(Context context, boolean marshall, int uid) {
        return ListUtil.copyToArrayList(
                CursorUtil.readCursorAs_final(
                        XProxyContent.luaQuery(context, marshall ? "getConfigs2" : "getConfigs", SQLSnake.create().whereColumn("uid", uid).asSnake()),
                        marshall, XPConfig.class));
    }
}
