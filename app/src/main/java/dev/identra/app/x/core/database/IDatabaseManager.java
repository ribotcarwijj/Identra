package dev.identra.app.x.core.database;

import android.content.Context;

import dev.identra.app.x.core.database.sql.SQLDatabase;

@SuppressWarnings("all")
public interface IDatabaseManager {
    SQLDatabase getDatabase(Context context);
    boolean initializeDatabase(Context context, boolean checkIsReady);
    public void reset(boolean setDatabaseNull);
}
