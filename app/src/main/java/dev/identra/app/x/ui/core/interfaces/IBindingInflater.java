package dev.identra.app.x.ui.core.interfaces;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.viewbinding.ViewBinding;

public interface IBindingInflater<TBinding extends ViewBinding> {
    TBinding inflate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent);
}
