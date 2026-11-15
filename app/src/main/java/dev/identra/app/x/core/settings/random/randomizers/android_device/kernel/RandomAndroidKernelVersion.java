package dev.identra.app.x.core.settings.random.randomizers.android_device.kernel;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;

public class RandomAndroidKernelVersion extends RandomElement {
    public RandomAndroidKernelVersion() {
        super("Android Kernel Version String");
        putSettings(RandomizersCache.SETTING_ANDROID_KERNEL_VERSION);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RanAndUtils.kernelVersionString(context));
    }
}