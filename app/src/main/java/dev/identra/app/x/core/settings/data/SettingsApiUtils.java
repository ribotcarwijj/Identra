package dev.identra.app.x.core.settings.data;

import android.util.Log;

import dev.identra.app.x.core.PacketBase;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.DatabaseHelpEx;
import dev.identra.app.x.core.database.sql.SQLDatabase;

public class SettingsApiUtils {
    private static final String TAG = "Identra.SettingsApiUtils";

    public static final String THEME_DARK = "dark";
    public static final String THEME_LIGHT = "light";

    public static String ensureIsTheme(String theme) {
        if(!THEME_DARK.equalsIgnoreCase(theme) && !THEME_LIGHT.equalsIgnoreCase(theme))
            return THEME_DARK;

        return theme;
    }

    public static A_CODE resultToCode(boolean result) {
        return result ? A_CODE.SUCCESS : A_CODE.GENERIC_DB_ERROR_M;
    }

    public static A_CODE ensureRead(SQLDatabase db, SettingPacket packet) {
        A_CODE code = PacketBase.ensurePacket(packet);
        if(code != A_CODE.NONE) {
            Log.e(TAG, "Failed to ensure Setting Packet is valid!");
            return code;
        }

        if(db == null) {
            Log.e(TAG, "Database input for setting action is null! [0x1] ");
            return A_CODE.GENERIC_DB_ERROR_X;
        }

        boolean ret = DatabaseHelpEx.prepareDatabase(db, SettingsApi.DATABASE_TABLE_NAME, SettingPacket.COLUMNS);
        if(!ret) {
            Log.e(TAG, "Failed to Prepare Settings Database / Table! ");
            return A_CODE.GENERIC_DB_ERROR_X;
        }

        return A_CODE.NONE;
    }
}
