package dev.identra.app.x.data.interfaces;

public interface IConditioner<T> {
    boolean meetsCondition(T value);
    T recondition(T value);
}
