package dev.identra.app.x.core.settings.random.randomizers.hardware.SoC;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomSocBoardManufacturer extends RandomElement {
    public RandomSocBoardManufacturer() {
        super("Hardware SoC Board Manufacturer");
        putSettings(RandomizersCache.SETTING_SOC_BOARD_MANUFACTURER);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RanHwdUtils.socCodeNames(context));
    }
}