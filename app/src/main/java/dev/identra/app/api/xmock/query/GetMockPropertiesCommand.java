package dev.identra.app.api.xmock.query;

import android.content.Context;
import android.database.Cursor;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.properties.MockPropPacket;
import dev.identra.app.api.xmock.provider.MockPropProvider;
import dev.identra.app.api.xstandard.QueryCommandHandler;
import dev.identra.app.api.xstandard.command.QueryPacket_old;
import dev.identra.app.utilities.CursorUtil;

public class GetMockPropertiesCommand extends QueryCommandHandler {
    public static GetMockPropertiesCommand create(boolean marshall) { return new GetMockPropertiesCommand(marshall); }

    @SuppressWarnings("unused")
    public GetMockPropertiesCommand() { this(false); }
    public GetMockPropertiesCommand(boolean marshall) {
        this.marshall = marshall;
        this.name = marshall ? "getModifiedProperties2" : "getModifiedProperties";
        this.requiresPermissionCheck = false;
    }

    @Override
    public Cursor handle(QueryPacket_old commandData) throws Throwable {
        MockPropPacket packet = commandData.readFullPacketFrom(MockPropPacket.class, MockPropPacket.USER_QUERY_PACKET_ONE);
        if(packet == null) return null;
        packet.resolveUserID();
        return CursorUtil.toMatrixCursor(
                MockPropProvider.getSettingsForPackage(
                        commandData.getContext(), commandData.getDatabase(), packet), marshall, 0);
    }

    public static Cursor invoke(Context context, boolean marshall, MockPropPacket packet) {
        return XProxyContent.mockQuery(
                context,
                marshall ? "getModifiedProperties2" : "getModifiedProperties",
                packet.generateSelectionArgsQuery(MockPropPacket.USER_QUERY_PACKET_ONE));
    }
}
