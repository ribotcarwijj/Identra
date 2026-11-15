package dev.identra.app.x.core.settings.random_old;

public enum RandomizerKind {
    GENERIC(0),
    OPTIONS(1),
    OPTIONS_PARENT_CONTROL(2);
    private final int value;
    RandomizerKind(int value) { this.value = value; }
    public int getValue() { return value; }
}
