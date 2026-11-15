package dev.identra.app.x.ui.core.interfaces;

import dev.identra.app.x.ui.adapters.ObjectState;
import dev.identra.app.x.ui.adapters.ExpandableStateFactory;

public interface IBindingView {
    String getId();
    void expand();
    void unExpand();

    void check();
    void unCheck();


    //boolean isChecked()


}
