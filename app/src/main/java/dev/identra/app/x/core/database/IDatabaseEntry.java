package dev.identra.app.x.core.database;

import android.content.ContentValues;
import android.database.Cursor;

import dev.identra.app.x.core.database.sql.SQLQueryBuilder;

public interface IDatabaseEntry {
    void populateContentValues(ContentValues cv);
    void populateFromContentValues(ContentValues cv);

    ContentValues toContentValues();

    void fromCursor(Cursor c);


    void populateSnake(SQLQueryBuilder snake);
}
