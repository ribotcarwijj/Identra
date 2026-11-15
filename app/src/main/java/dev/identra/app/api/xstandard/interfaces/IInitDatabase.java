package dev.identra.app.api.xstandard.interfaces;

import android.content.Context;

import dev.identra.app.XDatabaseOld;

public interface IInitDatabase {
    XDatabaseOld getDatabase(Context context);
    boolean initDatabase(Context context, boolean checkIsReady);
    void reset(boolean setDatabaseNull);
}
