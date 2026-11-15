package dev.identra.app.x.core.settings.random.randomizers.hardware.SoC;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomSocCpuProcessorCount extends RandomElement {
    public RandomSocCpuProcessorCount() {
        super("Hardware Processor Count");
        putSettings(RandomizersCache.SETTING_SOC_CPU_PROCESSOR_COUNT);
        putOptions(RandomOptionInt.generateRange(0, 6));    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}