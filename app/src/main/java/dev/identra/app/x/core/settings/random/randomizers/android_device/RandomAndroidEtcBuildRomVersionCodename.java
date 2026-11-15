package dev.identra.app.x.core.settings.random.randomizers.android_device;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;

/*
    ToDo:
 */
public class RandomAndroidEtcBuildRomVersionCodename extends RandomElement {
    public RandomAndroidEtcBuildRomVersionCodename() {
        super("Android ETC ROM Version Codename");
        putSettings(RandomizersCache.SETTING_ANDROID_ETC_BUILD_ROM_VERSION_CODENAME);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RanAndUtils.romVersionCodename(context));
    }
}