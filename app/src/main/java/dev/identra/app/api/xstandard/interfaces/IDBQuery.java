package dev.identra.app.api.xstandard.interfaces;

import dev.identra.app.XDatabaseOld;
import dev.identra.app.api.xstandard.database.SqlQuerySnake;

public interface IDBQuery {
    SqlQuerySnake createQuery(XDatabaseOld db);
}
