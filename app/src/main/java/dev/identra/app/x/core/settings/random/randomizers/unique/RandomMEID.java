package dev.identra.app.x.core.settings.random.randomizers.unique;

import java.util.Arrays;
import java.util.List;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomMEID extends RandomElement {
    public RandomMEID() {
        super("CELL MEID");
        putIndexSettings(RandomizersCache.SETTING_UNIQUE_MEID, 1, 2);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushSpecial(context.stack.pop(), RandomGenerator.nextStringNumeric(14));
    }
}
