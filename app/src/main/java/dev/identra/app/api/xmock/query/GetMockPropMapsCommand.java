package dev.identra.app.api.xmock.query;

import android.content.Context;
import android.database.Cursor;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.xmock.provider.MockPropProvider;
import dev.identra.app.api.xstandard.QueryCommandHandler;
import dev.identra.app.api.xstandard.command.QueryPacket_old;
import dev.identra.app.utilities.CursorUtil;

public class GetMockPropMapsCommand extends QueryCommandHandler {
    @SuppressWarnings("unused")
    public GetMockPropMapsCommand() { this(false); }
    public GetMockPropMapsCommand(boolean marshall) {
        this.marshall = marshall;
        this.name = marshall ? "getMockPropMaps2" : "getMockPropMaps";
        this.requiresPermissionCheck = false;
    }

    @Override
    public Cursor handle(QueryPacket_old commandData) throws Throwable {
        return CursorUtil.toMatrixCursor(
                MockPropProvider.getMockPropMaps(
                        commandData.getContext(),
                        commandData.getDatabase()), marshall, 0);
    }

    public static Cursor invoke(Context context, boolean marshall) {
        return XProxyContent.mockQuery(
                context,
                marshall ? "getMockPropMaps2" : "getMockPropMaps");
    }
}
