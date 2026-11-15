package dev.identra.app.x.core.settings.random.randomizers.android_device;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;

public class RandomAndroidBuildFingerprint extends RandomElement {
    public RandomAndroidBuildFingerprint() {
        super("Android Build Fingerprint");
        putSettings(RandomizersCache.SETTING_ANDROID_BUILD_FINGERPRINT);
    }

    @Override
    public void randomize(RandomizerSessionContext context) { context.pushValue(context.stack.pop(), RanAndUtils.fingerprint(context)); }
}