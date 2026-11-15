package dev.identra.app.x.core.settings.test;

import android.app.Activity;

import androidx.fragment.app.Fragment;

import java.util.Stack;
import java.util.UUID;

import dev.identra.app.x.ui.core.view_registry.IIdentifiableObject;
import dev.identra.app.x.core.settings.test.interfaces.IUIViewControl;

public class SharedSpace {
    public final String identifier = UUID.randomUUID().toString();

    //Lets try store EVERYTHIGN in there ?
    //Apps
    //Assignments
    //etc

    private Activity _baseActivity;
    private Fragment _fragment;



    private IUIViewControl _baseView = null;
    private StateMap _stateMap = null;
    private Stack<IIdentifiableObject> _viewStack = null;

    private final SupMap<StateMap> _states = new SupMap<>(StateMap.class);


    //public void setFocus(IUIView view) {
    //    _stateMap = _states.focus(view.getId());
    //    _viewStack =
    //}



}
