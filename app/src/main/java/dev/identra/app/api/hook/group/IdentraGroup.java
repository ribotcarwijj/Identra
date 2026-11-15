package dev.identra.app.api.hook.group;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.util.List;

import dev.identra.app.api.xstandard.interfaces.IDBSerial;
import dev.identra.app.utilities.CursorUtil;

public class IdentraGroup extends IdentraGroupBase implements IDBSerial {
    public IdentraGroup() {  }
    public IdentraGroup(String packageName, Integer uid, String name, Long used) { super(packageName, uid, name, used); }

    @Override
    public ContentValues createContentValues() {
        Log.i("Identra.IdentraGroup", "PKG=" + packageName + " UID=" + uid + " NAME=" + name + " USED=" + used);
        ContentValues cv = new ContentValues();
        cv.put("package", packageName);
        cv.put("uid", uid);
        cv.put("name", name);
        cv.put("used", used);
        return cv;
    }

    @Override
    public List<ContentValues> createContentValuesList() { return null; }

    @Override
    public void fromContentValuesList(List<ContentValues> contentValues) { }

    @Override
    public void fromContentValues(ContentValues contentValue) { }

    @Override
    public void fromCursor(Cursor cursor) {
        this.packageName = CursorUtil.getString(cursor, "package");
        this.uid = CursorUtil.getInteger(cursor, "uid", -1);
        this.name = CursorUtil.getString(cursor, "name");
        this.used = CursorUtil.getLong(cursor, "used");
    }
}
