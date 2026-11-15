package dev.identra.app.api.xstandard.interfaces;

public interface IDividerKind {
    String getDividerID(int position);
    String getLongID(int position);
    boolean isSearching();
    boolean hasChanged();
    void resetHashChanged();
}
