package dev.identra.app.api.core.query;

import android.content.Context;
import android.database.Cursor;
import android.os.Binder;

import java.util.Map;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.xstandard.QueryCommandHandler;
import dev.identra.app.api.xstandard.command.QueryPacket_old;
import dev.identra.app.api.core.provider.IdentraAppProvider;
import dev.identra.app.utilities.CursorUtil;

import dev.identra.app.api.app.IdentraApp;


public class GetAppsCommand extends QueryCommandHandler {
    @SuppressWarnings("unused")
    public GetAppsCommand() { this(false); }
    public GetAppsCommand(boolean marshall) {
        name = marshall ? "getApps2" : "getApps";
        this.marshall = marshall;
        requiresPermissionCheck = false;
        this.requiresSingleThread = true;
    }

    @Override
    public Cursor handle(QueryPacket_old commandData) throws Throwable {
        Map<String, IdentraApp> apps =
                IdentraAppProvider.getApps(
                        commandData.getContext(),
                        commandData.getDatabase(),
                        Binder.getCallingUid(),
                        true,
                        true);
        return CursorUtil.toMatrixCursor(
                apps.values(),
                marshall,
                0);
    }

    public static Cursor invoke(Context context, boolean marshall) {
        return XProxyContent.luaQuery(
                context,
                marshall ? "getApps2" : "getApps");
    }
}
