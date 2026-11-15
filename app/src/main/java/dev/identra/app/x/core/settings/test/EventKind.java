package dev.identra.app.x.core.settings.test;

import dev.identra.app.x.core.settings.NameInformation;
import dev.identra.app.x.core.settings.NameInformationKind;

public enum EventKind {
    UNKNOWN(-1),
    UPDATE(0),
    PUSH(1),
    CHECK(2),

    REMOVE(2);

    private final int value;
    EventKind(int value) { this.value = value; }
    public int getValue() { return value; }
}
