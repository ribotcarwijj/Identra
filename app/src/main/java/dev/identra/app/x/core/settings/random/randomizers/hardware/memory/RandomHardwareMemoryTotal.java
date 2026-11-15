package dev.identra.app.x.core.settings.random.randomizers.hardware.memory;

import dev.identra.app.x.data.utils.DateUtils;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomHardwareMemoryTotal extends RandomElement {
    public RandomHardwareMemoryTotal() {
        super("Hardware Memory Total (GB)");
        putSettings(RandomizersCache.SETTING_HARDWARE_MEMORY_TOTAL);
        putOptions(RandomOptionInt.generateRange(1, 26));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}