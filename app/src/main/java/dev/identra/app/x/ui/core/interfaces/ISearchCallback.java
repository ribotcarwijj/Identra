package dev.identra.app.x.ui.core.interfaces;

/**
 * 1.5.8: callback used by the main drawer/search bar so list screens can filter.
 * Reconstructed from the 1.5.8 build (see docs/RECONSTRUCT-158.md).
 */
public interface ISearchCallback {
    default void clearSearchFilter() { }
    void onSearch(String query);
}
