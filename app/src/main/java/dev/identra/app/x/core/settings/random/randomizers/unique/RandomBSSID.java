package dev.identra.app.x.core.settings.random.randomizers.unique;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanUnqUtils;

public class RandomBSSID extends RandomElement {
    public RandomBSSID() {
        super("Wifi BSSID");
        putSettings(RandomizersCache.SETTING_UNIQUE_NET_BSSID);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RanUnqUtils.mac(context));
    }
}