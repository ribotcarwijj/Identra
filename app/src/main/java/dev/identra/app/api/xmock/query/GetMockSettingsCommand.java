package dev.identra.app.api.xmock.query;

import android.content.Context;
import android.database.Cursor;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.settings.LuaSettingPacket;
import dev.identra.app.api.xmock.database.LuaSettingsManager;
import dev.identra.app.api.xstandard.QueryCommandHandler;
import dev.identra.app.api.xstandard.UserIdentityPacket;
import dev.identra.app.api.xstandard.command.QueryPacket_old;
import dev.identra.app.utilities.CursorUtil;

public class GetMockSettingsCommand extends QueryCommandHandler {

    @SuppressWarnings("unused")
    public GetMockSettingsCommand() { this(false); }
    public GetMockSettingsCommand(boolean marshall) {
        this.marshall = marshall;
        this.name = marshall ? "getMockSettings2" : "getMockSettings";
        this.requiresPermissionCheck = false;
    }

    @Override
    public Cursor handle(QueryPacket_old commandData) throws Throwable {
        LuaSettingPacket packet = commandData.readFullPacketFrom(LuaSettingPacket.class, UserIdentityPacket.USER_QUERY_PACKET_ONE);
        if(packet == null) return null;
        packet.resolveUserID();
        packet.ensureCode(LuaSettingPacket.CODE_GET_MODIFIED);
        switch (packet.getCode()) {
            case LuaSettingPacket.CODE_GET_ALL:
                return CursorUtil.toMatrixCursor(LuaSettingsManager.getAllSettings(
                        commandData.getContext(),
                        commandData.getDatabase(), packet), marshall, 0);
            case LuaSettingPacket.CODE_GET_MODIFIED:
                return CursorUtil.toMatrixCursor(LuaSettingsManager.getSettings(
                        commandData.getDatabase(),
                        packet), marshall, 0);
        } return null;
    }

    public static Cursor invoke(Context context, boolean marshall, LuaSettingPacket packet) {
        return XProxyContent.luaQuery(
                context,
                marshall ? "getMockSettings2" : "getMockSettings",
                packet.generateSelectionArgsQuery(UserIdentityPacket.USER_QUERY_PACKET_ONE));
    }
}