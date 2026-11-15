package dev.identra.app.x.core.settings.random.randomizers.android_device;

import dev.identra.app.x.data.utils.ArrayUtils;
import dev.identra.app.x.runtime.BuildInfo;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomAndroidBuildVersionMinSdk  extends RandomElement {
    public RandomAndroidBuildVersionMinSdk() {
        super("Android Build Version Min SDK");
        putSettings(RandomizersCache.SETTING_ANDROID_BUILD_VERSION_MIN_SDK);
        putOptions(RandomOptionInt.generateOptions(true, ArrayUtils.generate(6, 15, (v) -> BuildInfo.getApiLevelFromVersion((double) v))));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //ToDO make more advance
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}