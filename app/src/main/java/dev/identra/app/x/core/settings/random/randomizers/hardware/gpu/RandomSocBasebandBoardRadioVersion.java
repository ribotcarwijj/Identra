package dev.identra.app.x.core.settings.random.randomizers.hardware.gpu;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.randomizers.hardware.gpu.ran.RanGpuUtils;

public class RandomSocBasebandBoardRadioVersion extends RandomElement {
    public RandomSocBasebandBoardRadioVersion() {
        super("Hardware Base Band Board Radio Version");
        putSettings(RandomizersCache.SETTING_SOC_BASEBAND_BOARD_RADIO_VERSION);
    }

    @Override
    public void randomize(RandomizerSessionContext context) { context.pushValue(context.stack.pop(), RanGpuUtils.socBasebandBoardRadioVersion(context)); }
}
