package dev.identra.app.x.core.settings.random.randomizers.hardware.etc;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomHardwareFpSensorName extends RandomElement {
    public RandomHardwareFpSensorName() {
        super("Hardware ETC FP Sensor Name");
        putSettings(RandomizersCache.SETTING_HARDWARE_FP_SENSOR_NAME);
        putOptions(RandomOptionString.generate(RanHwdUtils.FINGERPRINT_MANUFACTURERS));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}