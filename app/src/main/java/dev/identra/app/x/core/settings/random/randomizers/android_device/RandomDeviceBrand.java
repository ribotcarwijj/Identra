package dev.identra.app.x.core.settings.random.randomizers.android_device;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanDevUtils;

public class RandomDeviceBrand extends RandomElement {
    public RandomDeviceBrand() {
        super("Device Brand Name");
        putSettings(RandomizersCache.SETTING_DEVICE_BRAND);
        //putParents(RandomizersCache.SETTING_PARENT_DEVICE);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RanDevUtils.brand(context));
    }
}