package dev.identra.app.x.core.settings.random.randomizers.android_device.kernel;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;

public class RandomAndroidKernelRelease extends RandomElement {
    public RandomAndroidKernelRelease() {
        super("Android Kernel Release");
        putSettings(RandomizersCache.SETTING_ANDROID_KERNEL_RELEASE);
        putOptions(RandomOptionString.generate(RanAndUtils.ANDROID_KERNEL_VERSION_RELEASES));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}