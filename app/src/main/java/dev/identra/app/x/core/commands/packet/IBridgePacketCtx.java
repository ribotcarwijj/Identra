package dev.identra.app.x.core.commands.packet;

import android.content.Context;

import dev.identra.app.x.core.database.sql.SQLDatabase;

public interface IBridgePacketCtx {
    String getMethod();
    Context getContext();
    SQLDatabase getDatabase();
    String getPackageName();
    String getCommandPrefix();
}
