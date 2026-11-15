package dev.identra.app.x.core.settings.random.randomizers.android_device.kernel;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;

public class RandomAndroidKernelNodeName extends RandomElement {
    public RandomAndroidKernelNodeName() {
        super("Android Kernel Node Name");
        putSettings(RandomizersCache.SETTING_ANDROID_KERNEL_NODE_NAME);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RanAndUtils.kernelNodeName(context));
    }
}