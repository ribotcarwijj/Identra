package dev.identra.app.x.core.commands.query;

import android.content.Context;
import android.database.Cursor;
import android.os.Binder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.app.IdentraApp;
import dev.identra.app.api.core.provider.IdentraAppProvider;
import dev.identra.app.api.xstandard.QueryCommandHandler;
import dev.identra.app.api.xstandard.command.QueryPacket_old;
import dev.identra.app.utilities.CursorUtil;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.core.commands.QueryCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.QueryPacket;
import dev.identra.app.x.core.hook.AppProviderApi;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.core.settings.data.SettingPacket;

@SuppressWarnings("unused")
public class GetAppsCommand extends QueryCommandHandlerEx {
    public GetAppsCommand() {
        this.name = "getAppsEx";
        this.requiresSingleThread = true;
        this.requiresPermissionCheck = true;
    }

    @Override
    public Cursor handle(QueryPacket commandData) throws Throwable {
        return CursorUtil.toMatrixCursor_final(
                AppProviderApi.getApps(commandData.getContext(),
                        commandData.getDatabase(),
                        Binder.getCallingUid(),
                        true,
                        true).values(),
                marshall,
                0);
    }

    public static Cursor invoke(Context context, boolean marshall) {
        return XProxyContent.luaQuery(
                context,
                marshall ? "getAppsEx2" : "getAppsEx");
    }

    public static List<AppXpPacket> get(Context context, boolean marshall) {
        return ListUtil.copyToArrayList(
                CursorUtil.readCursorAs_final(
                        XProxyContent.luaQuery(
                                context,
                                marshall ? "getAppsEx2" : "getAppsEx"),
                        marshall, AppXpPacket.class));
    }
}
