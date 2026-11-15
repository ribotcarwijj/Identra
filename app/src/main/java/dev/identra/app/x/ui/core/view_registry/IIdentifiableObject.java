package dev.identra.app.x.ui.core.view_registry;

import dev.identra.app.x.core.database.sql.SQLSnake;

public interface IIdentifiableObject {
    default String getObjectId() { return null; }
    default void setId(String id) { }


    default SQLSnake createSnake() { return null; }

    default boolean consumeObject(Object o) { return false; }

    default String getCategory() {
        return null;
    }
}
