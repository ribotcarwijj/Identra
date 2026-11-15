package dev.identra.app.x.core.settings.random.randomizers.unique;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.randomizers.hardware.gpu.ran.RanGpuUtils;
import dev.identra.app.x.core.settings.random.utils.RanUnqUtils;

public class RandomMac extends RandomElement {
    public RandomMac() {
        super("Unique MacAddress");
        putSettings(RandomizersCache.SETTING_UNIQUE_MAC);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RanUnqUtils.mac(context));
    }
}