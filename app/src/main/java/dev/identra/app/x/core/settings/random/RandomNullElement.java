package dev.identra.app.x.core.settings.random;

import dev.identra.app.x.core.settings.random.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;

public class RandomNullElement extends RandomElement {
    public static IRandomizer create() { return new RandomNullElement(); }
    public RandomNullElement() {
        super("[Select Randomizer]");
        putSettings("null");
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //Go through all randomizer and select one ? xD
    }
}