package dev.identra.app.x.core.settings.random.randomizers.hardware.etc;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomHardwareGpsModelName extends RandomElement {
    public RandomHardwareGpsModelName() {
        super("Hardware ETC GPS Model Name");
        putSettings(RandomizersCache.SETTING_HARDWARE_GPS_MODEL_NAME);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RanHwdUtils.gpsModelName(context));
    }
}