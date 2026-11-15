package dev.identra.app.x.core.settings.random.randomizers.hardware.etc;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomHardwareCameraApp extends RandomElement {
    public RandomHardwareCameraApp() {
        super("Hardware Camera Package Name");
        putSettings(RandomizersCache.SETTING_HARDWARE_CAMERA_APP);
        putOptions(RandomOptionString.generate(RanHwdUtils.CAMERA_PACKAGE_NAMES));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}