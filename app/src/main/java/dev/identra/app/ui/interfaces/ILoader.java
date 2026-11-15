package dev.identra.app.ui.interfaces;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import dev.identra.app.AppGeneric;

public interface ILoader {
    void loadData();
    void filter(String query);
    FragmentManager getManager();
    Fragment getFragment();
    AppGeneric getApplication();
}
