package dev.identra.app.x.core.settings.random.randomizers.settings;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomBootCount extends RandomElement {
    public RandomBootCount() {
        super("Boot Count");
        putSettings(RandomizersCache.SETTING_SETTING_BOOT_COUNT);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushSpecial(context.stack.pop(), String.valueOf(RandomGenerator.nextInt(3, 9999)));
    }
}