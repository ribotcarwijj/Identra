package dev.identra.app.x.core.configs;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.identra.app.DebugUtil;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.DatabaseHelpEx;
import dev.identra.app.x.core.database.sql.SQLDatabase;
import dev.identra.app.x.core.database.sql.SQLSnake;
import dev.identra.app.x.core.hook.AppProviderApi;
import dev.identra.app.x.core.settings.data.SettingPacket;
import dev.identra.app.x.core.settings.data.SettingsApiUtils;

public class ProfileApi {
    private static final String TAG = "Identra.ProfileApi";

    public static A_CODE single_locked(SQLDatabase database, AppProfile packet) { return DatabaseHelpEx.execute_one_locked_name(database, packet, AppProfile.TABLE_INFO); }


    public static A_CODE apply(Context context, SQLDatabase database, AppProfile packet) {
        if(DebugUtil.isDebug())
            Log.d(TAG, Str.fm("Applying Profile [%s] to Application [%s]", packet.name, packet.getCategory()));

        boolean result = AppProviderApi.clearAppData(context, packet.getCategory());
        if(DebugUtil.isDebug())
            Log.d(TAG, Str.fm("Cleared App [%s] Data to Restore Profile [%s]. Success ? %s", packet.getCategory(), packet.name, result));

        return A_CODE.SUCCESS;
    }

    public static AppProfile get(SQLDatabase database, int userId, String category, String name) {
        return SQLSnake.create(database, AppProfile.TABLE_NAME)
                .ensureDatabaseIsReady()
                .whereIdentity(userId, category)
                .whereColumn(AppProfile.FIELD_NAME, name)
                .asSnake()
                .queryGetFirstAs(AppProfile.class, true, false);
    }

    public static Collection<AppProfile> getProfiles(SQLDatabase database, int userId, String category) {
        return SQLSnake.create(database, AppProfile.TABLE_NAME)
                .ensureDatabaseIsReady()
                .whereIdentity(userId, category)
                .asSnake()
                .queryAs(AppProfile.class, true, false);
    }

    public static Collection<AppProfile> getAllProfiles(SQLDatabase database) {
        return DatabaseHelpEx.getFromDatabase(
                database,
                AppProfile.TABLE_NAME,
                AppProfile.class,
                true);
    }
}
