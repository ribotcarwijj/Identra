package dev.identra.app.x.core.settings.random.randomizers.android_device;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;
import dev.identra.app.x.core.settings.random.utils.RanDevUtils;

public class RandomAndroidBuildVersion extends RandomElement {
    public RandomAndroidBuildVersion() {
        super("Android Build Version");
        putSettings(RandomizersCache.SETTING_ANDROID_BUILD_VERSION);
        putOptions(RandomOptionInt.generateRange(6, 15, true));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}