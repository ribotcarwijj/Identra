package dev.identra.app.x.core.settings.random.randomizers.hardware.etc;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.RandomOptionNullElement;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomHardwareEfuse extends RandomElement {
    public RandomHardwareEfuse() {
        super("Hardware ETC E-Fuse");
        putSettings(RandomizersCache.SETTING_HARDWARE_EFUSE);
        putOptions(RandomOptionNullElement.create(),
                RandomOptionString.create("Green Safe", "1"),
                RandomOptionString.create("Red Blown", "0"));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}