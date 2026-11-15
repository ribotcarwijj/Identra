package dev.identra.app.x.core.commands;

public interface IXCommand {
    boolean requiresPermissionCheck();
    boolean requiresSingleThread();
    boolean isMarshal();
    String getCommandName();
}
