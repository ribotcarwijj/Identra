package dev.identra.app.x.core.settings.random.randomizers.hardware.memory;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomHardwareMemoryAvailable extends RandomElement {
    public RandomHardwareMemoryAvailable() {
        super("Hardware Memory Available (GB)");
        putSettings(RandomizersCache.SETTING_HARDWARE_MEMORY_AVAILABLE);
        putOptions(RandomOptionInt.generateRange(1, 26));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}