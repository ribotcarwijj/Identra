package dev.identra.app.x.ui.core.interfaces;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import dev.identra.app.x.ui.core.view_registry.SharedRegistry;
import dev.identra.app.x.core.settings.test.SharedViewControl;

public interface IStateManager {
    //SharedViewControl getSharedViewControl();
    SharedRegistry getSharedRegistry();
    FragmentManager getFragmentMan();
    Fragment getAsFragment();
}
