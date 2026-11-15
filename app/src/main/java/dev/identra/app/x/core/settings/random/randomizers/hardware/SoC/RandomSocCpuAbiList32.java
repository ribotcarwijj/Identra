package dev.identra.app.x.core.settings.random.randomizers.hardware.SoC;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomSocCpuAbiList32 extends RandomElement {
    public RandomSocCpuAbiList32() {
        super("Hardware CPU ABI List 32");
        putSettings(RandomizersCache.SETTING_SOC_CPU_ABI_LIST_32);
        putOptions(RandomOptionString.generate(RanHwdUtils.CPU_ABI_LIST_32));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}