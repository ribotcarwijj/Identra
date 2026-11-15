package dev.identra.app.x.core.settings.random.randomizers.hardware.etc;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomHardwareNfcKind extends RandomElement {
    public RandomHardwareNfcKind() {
        super("Hardware ETC NFC Chip Kind");
        putSettings(RandomizersCache.SETTING_HARDWARE_NFC_KIND);
        putOptions(RandomOptionString.generate(RanHwdUtils.NFC_CONTROLLERS));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}