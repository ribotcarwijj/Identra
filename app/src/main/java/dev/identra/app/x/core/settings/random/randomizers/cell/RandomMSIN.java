package dev.identra.app.x.core.settings.random.randomizers.cell;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomMSIN extends RandomElement {
    public RandomMSIN() {
        super("CELL MSIN");
        putIndexSettings(RandomizersCache.SETTING_CELL_MSIN, 1, 2);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushSpecial(context.stack.pop(), RandomGenerator.nextStringNumeric(9));
    }
}