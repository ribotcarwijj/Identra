package dev.identra.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dev.identra.xposed.XposedBridge;

public class VXP extends ContentProvider {
    private static final String TAG = "Identra.VXP";

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        try {
            Log.i(TAG, "Call " + arg +
                    " uid=" + android.os.Process.myUid() +
                    " cuid=" + android.os.Binder.getCallingUid());

            return XCommandBridgeStatic.vxpCall(getContext(), arg, extras, method);
            //return XProvider.call(getContext(), arg, extras);
        }
        catch (Throwable ex) {
            Log.e(TAG, Log.getStackTraceString(ex));
            XposedBridge.log(ex);
            return null;
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        try {
            Log.i(TAG, "Query " + projection[0].split("\\.")[1] +
                    " uid=" + android.os.Process.myUid() +
                    " cuid=" + android.os.Binder.getCallingUid());

            String[] split = projection[0].split("\\.");
            String method = split[0];
            String arg = split[1];

            return XCommandBridgeStatic.vxpQuery(getContext(), arg, selectionArgs, method);
            //return XProvider.query(getContext(), projection[0].split("\\.")[1], selectionArgs);
        } catch (Throwable ex) {
            Log.e(TAG, Log.getStackTraceString(ex));
            XposedBridge.log(ex);
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
