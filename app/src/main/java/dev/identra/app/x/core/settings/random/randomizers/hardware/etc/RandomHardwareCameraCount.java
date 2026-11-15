package dev.identra.app.x.core.settings.random.randomizers.hardware.etc;

import dev.identra.app.x.data.utils.DateUtils;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomHardwareCameraCount extends RandomElement {
    public RandomHardwareCameraCount() {
        super("Hardware Camera Count");
        putSettings(RandomizersCache.SETTING_HARDWARE_CAMERA_COUNT);
        putOptions(RandomOptionInt.generateRange(0, 6));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}