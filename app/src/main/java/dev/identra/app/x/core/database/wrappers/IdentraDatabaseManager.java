package dev.identra.app.x.core.database.wrappers;

import android.content.Context;
import android.util.Log;

import dev.identra.xposed.XposedBridge;
import dev.identra.app.XLegacyCore;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.database.DatabaseUtils;
import dev.identra.app.x.core.database.IDatabaseManager;
import dev.identra.app.x.core.database.sql.SQLDatabase;
import dev.identra.app.x.core.database.updaters.DatabaseUpdater;

public class IdentraDatabaseManager implements IDatabaseManager {
    public static IdentraDatabaseManager create() { return new IdentraDatabaseManager(); }

    private static final String TAG = LibUtil.generateTag(IdentraDatabaseManager.class);

    private final Object lock = new Object();
    private SQLDatabase db;
    private boolean init = false;

    public IdentraDatabaseManager() {  }

    @Override
    public SQLDatabase getDatabase(Context context) {
        initializeDatabase(context, true);
        return db;
    }

    @Override
    public boolean initializeDatabase(Context context, boolean checkIsReady) {
        synchronized (lock) {
            return internalInitializeDatabase(context, checkIsReady);
        }
    }

    private boolean internalInitializeDatabase(Context context, boolean checkIsReady) {
        if(checkIsReady && db != null) {
            if(!DatabaseUtils.isReady(db))
                reset(true);
        }

        try {
            if(db == null) {
                db = new SQLDatabase(SQLDatabase.DATABASE_X_LUA, context, true);
                XposedBridge.log(Str.fm("Opened Database, Database=%s", Str.noNL(db)));
                reset(false);
                if(!db.isOpen(true))
                    return false;
            }

            if(!init && db.isOpen(true)) {
                init = true;
                IdentraDatabaseHelp.ensureIsUpdated_legacy(db);
                dev.identra.app.x.core.database.CollectionMigration.run(db);
                XLegacyCore.initializeFromJsons(context, db, true);
                //XLegacyCore.loadHooksEx(context, db);
                DatabaseUpdater.ensureUpdated(context, db);
                //GlobalDatabaseResolver.initEnsureFunctions(context, db);
                //DatabaseUpdater<SettingReMappedItem.Setting_legacy>
            }
        }catch (Throwable e) {
            Log.e(TAG, Str.fm("Error Init for Identra Database! Database=%s  Error=%s", db, e));
            return false;
        }

        return init;
    }

    @Override
    public void reset(boolean setDatabaseNull) {
        XposedBridge.log("Resetting Database (xl), Set Database to null: " + String.valueOf(setDatabaseNull));
        if(setDatabaseNull && db != null) {
            if(db.isOpen())
                db.close();

            db = null;
        }

        init = false;
    }
}
