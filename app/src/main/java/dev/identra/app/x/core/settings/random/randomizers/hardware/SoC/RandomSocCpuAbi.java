package dev.identra.app.x.core.settings.random.randomizers.hardware.SoC;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomSocCpuAbi extends RandomElement {
    public RandomSocCpuAbi() {
        super("Hardware CPU ABI");
        putSettings(RandomizersCache.SETTING_SOC_CPU_ABI);
        putOptions(RandomOptionString.generate(RanHwdUtils.CPU_ABI_SINGLES));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}