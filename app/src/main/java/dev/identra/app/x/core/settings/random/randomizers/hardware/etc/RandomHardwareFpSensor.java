package dev.identra.app.x.core.settings.random.randomizers.hardware.etc;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomHardwareFpSensor extends RandomElement {
    public RandomHardwareFpSensor() {
        super("Hardware ETC FP Sensor");
        putSettings(RandomizersCache.SETTING_HARDWARE_FP_SENSOR);
        putOptions(RandomOptionString.generate(RanHwdUtils.FINGERPRINT_SENSOR_INTERFACES));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}