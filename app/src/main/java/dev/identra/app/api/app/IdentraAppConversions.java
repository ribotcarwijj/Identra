package dev.identra.app.api.app;

import android.database.Cursor;
import android.os.Parcel;

import java.util.ArrayList;
import java.util.Collection;

import dev.identra.app.utilities.CursorUtil;

public class IdentraAppConversions {
    public static Collection<IdentraApp> fromCursor(Cursor cursor, boolean marshall, boolean close) {
        Collection<IdentraApp> apps = new ArrayList<>();
        if(cursor == null) return apps;

        try {
            if(marshall) {
                while (cursor.moveToNext()) {
                    byte[] marshaled = cursor.getBlob(0);
                    Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(marshaled, 0, marshaled.length);
                    parcel.setDataPosition(0);
                    IdentraApp app = IdentraApp.CREATOR.createFromParcel(parcel);
                    parcel.recycle();
                    apps.add(app);
                }
            }else {

            }
        }finally {
            CursorUtil.closeCursor(cursor);
        }

        return apps;
    }
}
