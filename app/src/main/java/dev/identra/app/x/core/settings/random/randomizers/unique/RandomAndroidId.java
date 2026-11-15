package dev.identra.app.x.core.settings.random.randomizers.unique;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanUnqUtils;

public class RandomAndroidId extends RandomElement {
    public RandomAndroidId() {
        super("Unique Android ID");
        putSettings(RandomizersCache.SETTING_UNIQUE_ANDROID_ID, RandomizersCache.SETTING_XI_MI_HEALTH_ID);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RandomGenerator.nextStringHex(16).toLowerCase());
    }
}