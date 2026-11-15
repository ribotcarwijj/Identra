package dev.identra.app.x.core.settings.random_old.extra;

import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class OptionElement extends RandomElement {
    public static IRandomizer create(String option) { return new OptionElement(option); }
    public OptionElement(String option) {
        super(option);
    }

    @Override
    public String generateString() { return getDisplayName(); }
}
