package dev.identra.app.x.core.configs;

import android.util.Log;

import java.util.Collection;

import dev.identra.app.DebugUtil;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.XPacket;
import dev.identra.app.x.core.commands.call.GetSettingExCommand;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.ActionFlag;
import dev.identra.app.x.core.database.DatabaseHelpEx;
import dev.identra.app.x.core.database.sql.SQLDatabase;
import dev.identra.app.x.core.database.sql.SQLSnake;
import dev.identra.app.x.core.settings.data.SettingPacket;
import dev.identra.app.x.core.settings.data.SettingsApi;
import dev.identra.app.x.core.settings.data.SettingsApiUtils;

public class ConfigApi {
    private static final String TAG = "Identra.ConfigApi";

    public static A_CODE single_locked(SQLDatabase db, XPacket<XPConfig> packet) {
        if(DebugUtil.isDebug())
            Log.d(TAG, "Config Api Action Single, Config=" + Str.toStringOrNull(packet));


        if(packet.extra == null)
            return A_CODE.FAILED;

        A_CODE code = DatabaseHelpEx.ensureRead(db, XPConfig.TABLE_INFO);
        if(code != A_CODE.NONE)
            return code;

        switch (packet.code) {
            case UPDATE:
            case PUSH:
                code = SettingsApiUtils.resultToCode(DatabaseHelpEx.insertItem(db, XPConfig.TABLE_NAME, packet.extra));
                break;
            case DELETE:
                code = SettingsApiUtils.resultToCode(DatabaseHelpEx.deleteItem(
                        SQLSnake.create(db, XPConfig.TABLE_NAME)
                                //.whereIdentity(packet.getUserId(), packet.getPackageName())
                                .whereColumn(XPConfig.FIELD_USER, packet.getUserId())
                                .whereColumn(XPConfig.FIELD_NAME, packet.extra.name)
                                .asSnake()));
                break;
            case APPLY:
                //SettingsApi.

                break;
        }

        //Do Is Kill ?

        return code;
    }

    public static Collection<XPConfig> getConfigs(SQLDatabase database, int user) {
        return SQLSnake.create(database, XPConfig.TABLE_NAME)
                .ensureDatabaseIsReady()
                .ensureTableIsAvailable(XPConfig.TABLE_INFO)
                .whereColumn(XPConfig.FIELD_USER, user)
                .asSnake()
                .queryAs(XPConfig.class, true, false);
    }

    public static Collection<XPConfig> getAllConfigs(SQLDatabase database) {
        return DatabaseHelpEx.getFromDatabase(
                database,
                XPConfig.TABLE_NAME,
                XPConfig.class,
                true);
    }
}
