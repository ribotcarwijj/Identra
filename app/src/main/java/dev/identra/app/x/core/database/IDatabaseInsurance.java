package dev.identra.app.x.core.database;

import android.content.Context;

import dev.identra.app.XDatabaseOld;

public interface IDatabaseInsurance {
    String getInsuranceId();
    void ensureIsInitialized(Context context, XDatabaseOld database);
}
