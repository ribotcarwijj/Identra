package dev.identra.app.x.core.identity;

import dev.identra.app.x.ui.core.view_registry.IIdentifiableObject;
import dev.identra.app.x.core.interfaces.ICursorType;

public interface IUidCompress extends ICursorType, IIdentifiableObject {
    String getObjectId();     //Move this
    String getCategory();
    int getUid();
}
