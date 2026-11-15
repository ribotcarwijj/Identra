package dev.identra.app.x.core.settings.random_old.extra;

import dev.identra.app.x.hook.interceptors.build.random.RandomBaseOs;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class OptionFiller extends RandomElement {
    public static final IRandomizer INSTANCE = create();
    public static IRandomizer create() { return new RandomBaseOs(); }

    public OptionFiller() { super("Select Option"); }

    //Something null ?
    @Override
    public String generateString() { return ""; }
}
