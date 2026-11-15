package dev.identra.app.x.core.settings.random.randomizers.cell;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanUnqUtils;

public class RandomMCC extends RandomElement {
    public RandomMCC() {
        super("CELL MCC");
        putIndexSettings(RandomizersCache.SETTING_CELL_OPERATOR_MCC, 1, 2);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushSpecial(context.stack.pop(), String.valueOf(RandomGenerator.nextInt(100, 999)));
    }
}