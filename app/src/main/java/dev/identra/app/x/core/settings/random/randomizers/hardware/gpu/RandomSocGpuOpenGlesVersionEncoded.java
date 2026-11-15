package dev.identra.app.x.core.settings.random.randomizers.hardware.gpu;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.randomizers.hardware.gpu.ran.RanGpuUtils;

//GPU_GLES_VENDORS

public class RandomSocGpuOpenGlesVersionEncoded extends RandomElement {
    public RandomSocGpuOpenGlesVersionEncoded() {
        super("Hardware GPU OpenGLES Version Code");
        putSettings(RandomizersCache.SETTING_SOC_GPU_OPEN_GLES_VERSION_ENCODED);
        putOptions(RandomOptionString.generate(RanGpuUtils.GPU_GLES_VERSION_CODES));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}