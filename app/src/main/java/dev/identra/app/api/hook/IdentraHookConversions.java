package dev.identra.app.api.hook;

import android.database.Cursor;
import android.os.Parcel;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;

import dev.identra.app.DebugUtil;
import dev.identra.app.utilities.CursorUtil;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.core.LibUtil;


public class IdentraHookConversions {
    private static final String TAG = LibUtil.generateTag(IdentraHookConversions.class);


    public static Collection<XHook> fromCursor(Cursor cursor, boolean marshall, boolean close) {
        Collection<XHook> ps = new ArrayList<>();
        try {
            if(marshall) {
                while (cursor != null && cursor.moveToNext()) {
                    byte[] marshaled = cursor.getBlob(0);
                    Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(marshaled, 0, marshaled.length);
                    parcel.setDataPosition(0);
                    XHook hook = XHook.CREATOR.createFromParcel(parcel);
                    parcel.recycle();
                    ps.add(hook);
                }
            }else {

            }
        }finally {
            if(close) CursorUtil.closeCursor(cursor);
        }

        if(DebugUtil.isDebug())
            Log.d(TAG, "Converted Cursor to Hooks / Assignments Size=" + ListUtil.size(ps));

        return ps;
    }
}
