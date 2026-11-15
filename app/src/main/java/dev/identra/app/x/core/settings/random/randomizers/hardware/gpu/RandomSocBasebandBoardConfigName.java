package dev.identra.app.x.core.settings.random.randomizers.hardware.gpu;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.randomizers.hardware.gpu.ran.RanGpuUtils;
import dev.identra.app.x.core.settings.random.utils.RanDevUtils;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomSocBasebandBoardConfigName extends RandomElement {
    public RandomSocBasebandBoardConfigName() {
        super("Hardware Base Band Board Config");
        putSettings(RandomizersCache.SETTING_SOC_BASEBAND_BOARD_CONFIG_NAME);
    }

    @Override
    public void randomize(RandomizerSessionContext context) { context.pushValue(context.stack.pop(), RanGpuUtils.socBaseboardConfigName(context)); }
}
