package dev.identra.app.x.core.settings.random.randomizers.hardware.etc;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanHwdUtils;

public class RandomHardwareNfcControllerInterface extends RandomElement {
    public RandomHardwareNfcControllerInterface() {
        super("Hardware ETC NFC CTRL Interface");
        putSettings(RandomizersCache.SETTING_HARDWARE_NFC_CONTROLLER_INTERFACE);
        putOptions(RandomOptionString.generate(RanHwdUtils.NFC_CONTROLLER_INTERFACES));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}