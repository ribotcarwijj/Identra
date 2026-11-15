package dev.identra.app.x.core.commands.packet;

public interface IQueryPacketObject {
    void readSelectionFromQuery(String[] selection, int flags);
}
