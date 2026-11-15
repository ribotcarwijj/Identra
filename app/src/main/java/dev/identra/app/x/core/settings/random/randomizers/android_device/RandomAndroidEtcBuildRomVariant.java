package dev.identra.app.x.core.settings.random.randomizers.android_device;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;

public class RandomAndroidEtcBuildRomVariant  extends RandomElement {
    public RandomAndroidEtcBuildRomVariant() {
        super("Android ETC ROM Variant");
        putSettings(RandomizersCache.SETTING_ANDROID_ETC_BUILD_ROM_VARIANT);
        putOptions(RandomOptionString.generate(RanAndUtils.DEFAULT_BUILD_TAGS));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}