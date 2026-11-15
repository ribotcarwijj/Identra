package dev.identra.app.x.ui.core.interfaces;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import dev.identra.app.x.data.interfaces.IValidator;

public interface IGenericViewHolder<TElement extends IValidator, TBinding extends ViewBinding> {
    void wire();
    void unWire();

    void hide();
    void show();

    IGenericElementEvent<TElement, TBinding> getEvents();

    TElement getObject();
    TBinding getBinding();
}
