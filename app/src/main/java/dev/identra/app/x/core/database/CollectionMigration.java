package dev.identra.app.x.core.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.identra.IdentraNs;
import dev.identra.app.x.core.database.sql.SQLDatabase;

/**
 * 0.5.0: Identra ships one hook collection ("Identra"). Older installs stored hook ids and the
 * collection setting with the six collections it was merged from. Runs in system_server when the
 * database is opened, before the hook definitions are loaded. Idempotent: on an already migrated
 * database every statement matches nothing.
 *
 *  - global setting "collection" -> "Identra"
 *  - assignments  "PrivacyEx.X", "Analytics.X", "Hide.X", "Telephony.X", "Settings.X",
 *    "Modifications.X" -> "Identra.X" (kept per app; an existing Identra.X row wins)
 *  - assignments of the dropped sets ("Privacy.", "Log.", "Test.") are removed
 *  - user / edited hook definitions in the "hook" table get the same treatment
 * Per-app and global values (settings table) are not touched except the collection setting.
 */
public final class CollectionMigration {
    private static final String TAG = "Identra.Migration";

    public static final List<String> MERGED = Arrays.asList("PrivacyEx", "Analytics", "Hide", "Telephony", "Settings", "Modifications");
    public static final List<String> DROPPED = Arrays.asList("Privacy", "Log", "Test");

    /**
     * 0.5.0 property rules added to ID.Build. Apps that already had the Java Build spoof
     * (Build.MODEL/Rule assigned) get them too, so SystemProperties / getprop stay coherent with
     * the spoofed Build fields without re-applying the profile.
     */
    public static final List<String> NEW_BUILD_RULES = Arrays.asList(
            "Build.BRAND/Rule", "Build.DEVICE/Rule", "Build.PRODUCT/Rule", "Build.FINGERPRINT/Rule", "Build.ID/Rule",
            "Build.DISPLAY/Rule", "Version.INCREMENTAL/Rule", "Build.DESCRIPTION/Rule", "Build.HOST/Rule", "Build.USER/Rule",
            "Build.TAGS/Rule", "Build.TYPE/Rule", "Version.RELEASE/Rule", "Version.SECURITY_PATCH/Rule", "Build.TIME/Rule",
            "Build.DATE/Rule", "Build.BOOTLOADER/Rule", "Build.HARDWARE/Rule", "Build.RADIO/Rule");

    private CollectionMigration() { }

    /** Maps an old hook id to its Identra id; null when the hook was dropped; unchanged otherwise. */
    public static String mapHookId(String id) {
        if (id == null) return null;
        int dot = id.indexOf('.');
        if (dot <= 0) return id;
        String col = id.substring(0, dot);
        if (MERGED.contains(col)) return IdentraNs.COLLECTION + id.substring(dot);
        if (DROPPED.contains(col)) return null;
        return id;
    }

    public static void run(SQLDatabase database) {
        if (database == null) return;
        SQLiteDatabase db = database.getDatabase();
        if (db == null || !db.isOpen()) return;
        int moved = 0, dropped = 0, hooks = 0, added = 0;
        try {
            // settings: the collection setting
            if (tableExists(db, "settings")) {
                ContentValues cv = new ContentValues();
                cv.put("value", IdentraNs.COLLECTION);
                db.update("settings", cv, "name = ? AND (value IS NULL OR value != ?)", new String[]{"collection", IdentraNs.COLLECTION});
            }

            // assignments
            if (tableExists(db, "assignments")) {
                for (String col : MERGED) {
                    String prefix = col + ".";
                    // UPDATE OR REPLACE: an app that already has the Identra.X row keeps one row
                    db.execSQL("UPDATE OR REPLACE assignments SET hook = ? || substr(hook, ?) WHERE substr(hook, 1, ?) = ?",
                            new Object[]{IdentraNs.COLLECTION + ".", prefix.length() + 1, prefix.length(), prefix});
                    moved += changes(db);
                }
                for (String col : DROPPED) {
                    String prefix = col + ".";
                    dropped += db.delete("assignments", "substr(hook, 1, ?) = ?", new String[]{String.valueOf(prefix.length()), prefix});
                }
                String anchor = IdentraNs.COLLECTION + ".Build.MODEL/Rule";
                for (String rule : NEW_BUILD_RULES) {
                    String id = IdentraNs.COLLECTION + "." + rule;
                    db.execSQL("INSERT INTO assignments (user, category, hook, installed, used, restricted) "
                            + "SELECT DISTINCT a.user, a.category, ?, -1, -1, 0 FROM assignments a WHERE a.hook = ? "
                            + "AND NOT EXISTS (SELECT 1 FROM assignments b WHERE b.user = a.user AND b.category = a.category AND b.hook = ?)",
                            new Object[]{id, anchor, id});
                    added += changes(db);
                }
            }

            // hook definitions saved in the database (user hooks / edited copies)
            if (tableExists(db, "hook")) {
                List<String[]> rows = new ArrayList<>();
                Cursor c = null;
                try {
                    c = db.query("hook", new String[]{"id", "definition"}, null, null, null, null, null);
                    while (c != null && c.moveToNext()) rows.add(new String[]{c.getString(0), c.getString(1)});
                } finally {
                    if (c != null) c.close();
                }
                for (String[] r : rows) {
                    String id = r[0];
                    String mapped = mapHookId(id);
                    if (id == null || id.equals(mapped)) continue;
                    db.delete("hook", "id = ?", new String[]{id});
                    hooks++;
                    if (mapped == null) continue;
                    try {
                        JSONObject j = new JSONObject(r[1] == null ? "{}" : r[1]);
                        j.put("collection", IdentraNs.COLLECTION);
                        ContentValues cv = new ContentValues();
                        cv.put("id", mapped);
                        cv.put("definition", j.toString());
                        db.insertWithOnConflict("hook", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                    } catch (Throwable t) {
                        Log.w(TAG, "Hook definition " + id + " not migrated: " + t);
                    }
                }
            }
            Log.i(TAG, "Collection migration done: assignments moved=" + moved + " dropped=" + dropped + " rules added=" + added + " hook rows=" + hooks);
        } catch (Throwable t) {
            Log.e(TAG, "Collection migration failed: " + t);
        }
    }

    private static int changes(SQLiteDatabase db) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT changes()", null);
            return c != null && c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Throwable t) {
            return 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean tableExists(SQLiteDatabase db, String table) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{table});
            return c != null && c.moveToFirst();
        } catch (Throwable t) {
            return false;
        } finally {
            if (c != null) c.close();
        }
    }
}
