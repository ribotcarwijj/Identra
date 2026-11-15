package dev.identra.app.x.core.settings.random.randomizers.hardware.SoC;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomSocBoardConfigCodeName extends RandomElement {
    public RandomSocBoardConfigCodeName() {
        super("Hardware SoC Board Config Code");
        putSettings(RandomizersCache.SETTING_SOC_BOARD_CONFIG_CODE_NAME);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RanHwdUtils.socCodeNames(context));
    }
}