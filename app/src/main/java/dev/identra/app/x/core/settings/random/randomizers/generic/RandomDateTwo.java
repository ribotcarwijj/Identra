package dev.identra.app.x.core.settings.random.randomizers.generic;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;

public class RandomDateTwo extends RandomElement {
    public RandomDateTwo() {
        super("Random Date Two (YYYYMMDD)");
        putSettings(RandomizersCache.SETTING_ANDROID_BUILD_DATE_TWO);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        for (String setting : getSettings()) {
            context.pushValue(setting, RanAndUtils.dateTwo(context));
        }
    }
}