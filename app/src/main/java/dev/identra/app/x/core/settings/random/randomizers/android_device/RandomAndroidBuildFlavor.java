package dev.identra.app.x.core.settings.random.randomizers.android_device;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;

public class RandomAndroidBuildFlavor extends RandomElement {
    public RandomAndroidBuildFlavor() {
        super("Android Build Flavor");
        putSettings(RandomizersCache.SETTING_ANDROID_BUILD_FLAVOR);
    }

    @Override
    public void randomize(RandomizerSessionContext context) { context.pushValue(context.stack.pop(), RanAndUtils.flavor(context)); }
}