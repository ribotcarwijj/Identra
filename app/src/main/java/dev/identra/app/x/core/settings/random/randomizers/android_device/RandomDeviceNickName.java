package dev.identra.app.x.core.settings.random.randomizers.android_device;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanDevUtils;

public class RandomDeviceNickName extends RandomElement {
    public RandomDeviceNickName() {
        super("Device NickName");
        putSettings(RandomizersCache.SETTING_DEVICE_NICKNAME);
        //putParents(RandomizersCache.SETTING_PARENT_DEVICE);
    }

    @Override
    public void randomize(RandomizerSessionContext context) { context.pushValue(context.stack.pop(), RanDevUtils.nickName(context)); }
}