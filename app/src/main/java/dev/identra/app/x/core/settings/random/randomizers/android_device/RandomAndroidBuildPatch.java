package dev.identra.app.x.core.settings.random.randomizers.android_device;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;

public class RandomAndroidBuildPatch  extends RandomElement {
    public RandomAndroidBuildPatch() {
        super("Android Build Patch");
        putSettings(RandomizersCache.SETTING_ANDROID_BUILD_PATCH);
    }

    @Override
    public void randomize(RandomizerSessionContext context) { context.pushValue(context.stack.pop(), RanAndUtils.patch(context)); }
}