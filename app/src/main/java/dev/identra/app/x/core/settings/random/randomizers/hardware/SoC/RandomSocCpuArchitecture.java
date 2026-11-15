package dev.identra.app.x.core.settings.random.randomizers.hardware.SoC;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomSocCpuArchitecture extends RandomElement {
    public RandomSocCpuArchitecture() {
        //PROCESSOR_ARCHITECTURES
        super("Hardware CPU Architecture");
        putSettings(RandomizersCache.SETTING_SOC_CPU_ARCHITECTURE);
        putOptions(RandomOptionString.generate(RanHwdUtils.PROCESSOR_ARCHITECTURES));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}