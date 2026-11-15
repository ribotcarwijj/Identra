package dev.identra.app.api.xstandard.interfaces;

import android.content.Context;

import dev.identra.app.XDatabaseOld;

public interface IBridgePacketContext {
    String getCommandPrefix();
    String getMethod();
    Context getContext();
    XDatabaseOld getDatabase();
    String getPackageName();
}
