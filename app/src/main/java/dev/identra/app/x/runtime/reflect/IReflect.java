package dev.identra.app.x.runtime.reflect;

import dev.identra.app.x.data.interfaces.IValidator;

public interface IReflect extends IValidator {
    String getName();
    DynClass getClazz();
    void setAccessible(boolean accessible);
    Exception getLastException();
    boolean wasSuccessful();
}
