package dev.identra.app.x.core.settings.random.randomizers.hardware.SoC;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomSocBoardModel extends RandomElement {
    public RandomSocBoardModel() {
        super("Hardware SoC Model");
        putSettings(RandomizersCache.SETTING_SOC_BOARD_MODEL);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
         context.pushValue(context.stack.pop(), RanHwdUtils.socModel(context));
    }
}