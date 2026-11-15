package dev.identra.app.x.data.interfaces;

public interface IValueSelector<T> {
    //boolean isBad(T o);
    T select(T a, T b);
}
