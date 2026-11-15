package dev.identra.app.x.core.settings.test.interfaces;

public interface ICreateObject {
    default void setId(String id) { }
    default String getId() { return null; }
}
