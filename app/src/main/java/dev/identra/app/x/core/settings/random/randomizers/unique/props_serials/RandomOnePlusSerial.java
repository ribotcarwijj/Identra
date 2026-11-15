package dev.identra.app.x.core.settings.random.randomizers.unique.props_serials;


//IGFBDIAFCJIFIEJFG


import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomOnePlusSerial extends RandomElement {
    public RandomOnePlusSerial() {
        super("Unique OnePlus Serial NO");
        putSettings(RandomizersCache.SETTING_PROP_ONE_PLUS_UNIQUE_SERIAL);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RandomGenerator.nextStringAlpha(17).toUpperCase());
    }
}