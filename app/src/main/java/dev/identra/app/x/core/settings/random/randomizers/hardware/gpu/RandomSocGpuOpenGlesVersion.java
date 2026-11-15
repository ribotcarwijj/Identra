package dev.identra.app.x.core.settings.random.randomizers.hardware.gpu;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.randomizers.hardware.gpu.ran.RanGpuUtils;

public class RandomSocGpuOpenGlesVersion extends RandomElement {
    public RandomSocGpuOpenGlesVersion() {
        super("Hardware GPU OpenGLES Version String");
        putSettings(RandomizersCache.SETTING_SOC_GPU_OPEN_GLES_VERSION);
        putOptions(RandomOptionString.generate(RanGpuUtils.GPU_GLES_VERSION_STRINGS));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}