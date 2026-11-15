package dev.identra.app.x.core.settings.random.randomizers.android_device.kernel;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;

public class RandomAndroidKernelSysName extends RandomElement {
    public RandomAndroidKernelSysName() {
        super("Android Kernel SysName");
        putSettings(RandomizersCache.SETTING_ANDROID_KERNEL_SYS_NAME);
        putOptions(RandomOptionString.generate(RanAndUtils.SYS_MACHINES));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}