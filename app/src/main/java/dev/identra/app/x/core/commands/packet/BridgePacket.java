package dev.identra.app.x.core.commands.packet;

import android.content.Context;

import androidx.annotation.NonNull;

import dev.identra.app.x.data.string.StrBuilder;
import dev.identra.app.x.core.database.sql.SQLDatabase;

public class BridgePacket implements IBridgePacketCtx {
    protected String commandPrefix;
    protected String method;
    protected Context context;
    protected SQLDatabase db;
    protected String packageName;

    @Override
    public String getMethod() { return this.method; }
    @Override
    public Context getContext() { return this.context; }
    @Override
    public SQLDatabase getDatabase() { return this.db; }
    @Override
    public String getPackageName() { return this.packageName; }
    @Override
    public String getCommandPrefix() { return this.commandPrefix; }

    public BridgePacket() { }
    public BridgePacket(String commandPrefix, String method, Context context, SQLDatabase db, String packageName) {
        this.commandPrefix = commandPrefix;
        this.method = method;
        this.context = context;
        this.db = db;
        this.packageName = packageName;
        //Is VXP if package is null ?
        //Also secret key ?
    }

    @NonNull
    @Override
    public String toString() {
        return StrBuilder.create()
                .ensureOneNewLinePer(true)
                .appendFieldLine("Command Prefix", this.commandPrefix)
                .appendFieldLine("Method", this.method)
                .appendFieldLine("Package", this.packageName)
                .appendFieldLine("Database", this.db)
                .toString(true);
    }
}
