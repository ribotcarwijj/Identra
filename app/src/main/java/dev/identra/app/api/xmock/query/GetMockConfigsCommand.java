package dev.identra.app.api.xmock.query;

import android.content.Context;
import android.database.Cursor;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.xmock.database.MockConfigManager;
import dev.identra.app.api.xstandard.QueryCommandHandler;
import dev.identra.app.api.xstandard.command.QueryPacket_old;
import dev.identra.app.utilities.CursorUtil;

public class GetMockConfigsCommand extends QueryCommandHandler {
    public static GetMockConfigsCommand create(boolean marshall) { return new GetMockConfigsCommand(marshall); };

    @SuppressWarnings("unused")
    public GetMockConfigsCommand() { this(false); }
    public GetMockConfigsCommand(boolean marshall) {
        this.marshall = marshall;
        this.name = marshall ? "getMockConfigs2" : "getMockConfigs";
        this.requiresPermissionCheck = false;
    }

    @Override
    public Cursor handle(QueryPacket_old commandData) throws Throwable {
        return CursorUtil.toMatrixCursor(
                MockConfigManager.getMockConfigs(commandData.getContext(), commandData.getDatabase()),
                marshall, 0);
    }

    public static Cursor invoke(Context context, boolean marshall) {
        return XProxyContent.mockQuery(
                context,
                marshall ? "getMockConfigs2" : "getMockConfigs");
    }
}
