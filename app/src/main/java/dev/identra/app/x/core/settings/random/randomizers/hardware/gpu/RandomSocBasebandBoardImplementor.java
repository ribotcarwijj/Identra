package dev.identra.app.x.core.settings.random.randomizers.hardware.gpu;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.randomizers.hardware.gpu.ran.RanGpuUtils;

public class RandomSocBasebandBoardImplementor extends RandomElement {
    public RandomSocBasebandBoardImplementor() {
        super("Hardware Base Band Board Impl");
        putSettings(RandomizersCache.SETTING_SOC_BASEBAND_BOARD_IMPLEMENTOR);
    }

    @Override
    public void randomize(RandomizerSessionContext context) { context.pushValue(context.stack.pop(), RanGpuUtils.socBasebandBoardRadioVersion(context)); }
}
