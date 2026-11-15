package dev.identra.app.x.core.settings.random.randomizers.hardware.etc;

import dev.identra.app.x.data.utils.DateUtils;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomHardwareGpsModelYear extends RandomElement {
    public RandomHardwareGpsModelYear() {
        super("Hardware ETC GPS Model Year");
        putSettings(RandomizersCache.SETTING_HARDWARE_GPS_MODEL_YEAR);
        putOptions(RandomOptionInt.generateRange(2016, DateUtils.getCurrentYear()));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}