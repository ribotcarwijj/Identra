package dev.identra.app.x.core.root;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

public class RootHandler implements Handler.Callback {
    private static final String TAG = "Identra.RootHandler";

    public static RootHandler instance;

    public RootHandler() { instance = this; }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        Log.i(TAG, "handleMessage: " + msg.toString() + " what: " + msg.what);
        return false;
    }
}
