package dev.identra.app.x.core.settings.random.randomizers.hardware.gpu;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.randomizers.hardware.gpu.ran.RanGpuUtils;

public class RandomSocGpuOpenGlesRenderer extends RandomElement {
    public RandomSocGpuOpenGlesRenderer() {
        super("Hardware GPU OpenGLES Renderer");
        putSettings(RandomizersCache.SETTING_SOC_GPU_OPEN_GLES_RENDERER);
        putOptions(RandomOptionString.generate(RanGpuUtils.GPU_GLES_RENDERERS));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}