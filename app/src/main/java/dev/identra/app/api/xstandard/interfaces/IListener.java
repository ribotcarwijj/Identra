package dev.identra.app.api.xstandard.interfaces;

import android.content.Context;

public interface IListener {
    void onAssign(Context context, String groupName, boolean assign);
}
