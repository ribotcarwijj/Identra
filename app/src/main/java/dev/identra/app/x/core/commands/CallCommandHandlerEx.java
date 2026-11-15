package dev.identra.app.x.core.commands;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.string.StrBuilder;
import dev.identra.app.x.core.commands.packet.CallPacket;

@SuppressWarnings("all")
public abstract class CallCommandHandlerEx implements IXCommand {
    protected String name;
    protected boolean requiresPermissionCheck = false;
    protected boolean requiresSingleThread = false;
    protected boolean isLegacy = false;

    public CallCommandHandlerEx() { }

    public abstract Bundle handle(CallPacket packet) throws Throwable;

    @Override
    public boolean requiresPermissionCheck() { return requiresPermissionCheck; }

    @Override
    public boolean requiresSingleThread() { return requiresSingleThread; }

    @Override
    public boolean isMarshal() { return false; }

    @Override
    public String getCommandName() { return name; }

    @Override
    public boolean equals(@Nullable Object obj) { return Str.equalsObject(obj, this.name, true); }

    @NonNull
    @Override
    public String toString() {
        return StrBuilder.create()
                .appendFieldLine("Name", name)
                .appendFieldLine("Requires UID Check", String.valueOf(requiresPermissionCheck))
                .appendFieldLine("Single Thread", String.valueOf(requiresSingleThread))
                .toString(true);
    }
}
