package dev.identra.app.x.ui.core.interfaces;

import java.util.List;

public interface IListChange<T> {
    void onItemsChange(List<T> items);
}
