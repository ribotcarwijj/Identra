package dev.identra.app.x.core.database.wrappers;

import android.content.Context;

import dev.identra.xposed.XposedBridge;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.database.DatabaseUtils;
import dev.identra.app.x.core.database.IDatabaseManager;
import dev.identra.app.x.core.database.sql.SQLDatabase;

public class XMocDatabaseManager implements IDatabaseManager  {
    public static XMocDatabaseManager create() { return new XMocDatabaseManager(); }

    private final Object lock = new Object();
    private SQLDatabase db;
    private boolean init = false;

    public XMocDatabaseManager() { }

    @Override
    public SQLDatabase getDatabase(Context context) {
        initializeDatabase(context, true);
        return db;
    }

    @Override
    public boolean initializeDatabase(Context context, boolean checkIsReady) {
        synchronized (lock) {
            if(checkIsReady && db != null) {
                if(!DatabaseUtils.isReady(db))
                    reset(true);
            }

            if(db == null) {
                db = new SQLDatabase(SQLDatabase.DATABASE_MOCK, context, true);
                XposedBridge.log(Str.fm("Opened Database [2], Database=%s", Str.noNL(db)));
                reset(false);
                if(!db.isOpen(true))
                    return false;
            }

            if(!init && db.isOpen(true)) {
                //ensure all the tables are created and ready!
                init = true;
            }
        }

        return init;
    }

    @Override
    public void reset(boolean setDatabaseNull) {
        XposedBridge.log("Resetting Database (xm), Set Database to null: " + String.valueOf(setDatabaseNull));
        if(setDatabaseNull && db != null) {
            if(db.isOpen())
                db.close();

            db = null;
        }

        init = false;
    }
}
