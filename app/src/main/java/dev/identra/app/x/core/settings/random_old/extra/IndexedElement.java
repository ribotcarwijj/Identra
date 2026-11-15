package dev.identra.app.x.core.settings.random_old.extra;

import dev.identra.app.x.core.settings.random_old.RandomElement;

public class IndexedElement extends RandomElement {
    public IndexedElement(int index) {
        super(String.valueOf(index));
    }

    @Override
    public String generateString() { return getDisplayName(); }
}
