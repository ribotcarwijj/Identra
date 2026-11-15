package dev.identra.app.x.core.settings.random.randomizers.hardware.gpu;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.randomizers.hardware.gpu.ran.RanGpuUtils;

public class RandomSocGpuEglImplementor extends RandomElement {
    public RandomSocGpuEglImplementor() {
        super("Hardware GPU EGL Implementor");
        putSettings(RandomizersCache.SETTING_SOC_GPU_EGL_IMPLEMENTOR);
        putOptions(RandomOptionString.generate(RanGpuUtils.GPU_EGL_IMPLEMENTORS));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}