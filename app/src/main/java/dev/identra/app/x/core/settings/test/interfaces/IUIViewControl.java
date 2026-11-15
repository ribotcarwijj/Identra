package dev.identra.app.x.core.settings.test.interfaces;

import dev.identra.app.x.ui.core.view_registry.IIdentifiableObject;
import dev.identra.app.x.core.settings.test.EventTrigger;
import dev.identra.app.x.core.settings.test.SharedViewControl;

public interface IUIViewControl extends IIdentifiableObject {
    //String getId();
    //SharedSpace getShared();
    //void setShared(SharedSpace shared);

    SharedViewControl getSharedViewControl();
    void setSharedViewControl(SharedViewControl viewControl);

    default void onEvent(EventTrigger event) { }
    default void onView() {  }
    default void onClean() { }

    default boolean isView(String id) {
        return true;
    }
}
