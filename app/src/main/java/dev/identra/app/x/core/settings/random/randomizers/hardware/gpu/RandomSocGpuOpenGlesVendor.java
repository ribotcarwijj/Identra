package dev.identra.app.x.core.settings.random.randomizers.hardware.gpu;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.randomizers.hardware.gpu.ran.RanGpuUtils;

public class RandomSocGpuOpenGlesVendor extends RandomElement {
    public RandomSocGpuOpenGlesVendor() {
        super("Hardware GPU OpenGLES Vendor");
        putSettings(RandomizersCache.SETTING_SOC_GPU_OPEN_GLES_VENDOR);
        putOptions(RandomOptionString.generate(RanGpuUtils.GPU_GLES_VENDORS));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}