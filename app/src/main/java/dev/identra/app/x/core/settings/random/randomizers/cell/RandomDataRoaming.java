package dev.identra.app.x.core.settings.random.randomizers.cell;

import android.telephony.SubscriptionManager;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomOptionNullElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomDataRoaming extends RandomElement {
    public RandomDataRoaming() {
        super("Data Roaming Bit Flag");
        putIndexSettings(RandomizersCache.SETTING_CELL_DATA_ROAMING, 1, 2);
        putOptions(
                RandomOptionNullElement.create(),
                RandomOptionInt.create("Enabled (" + SubscriptionManager.DATA_ROAMING_ENABLE + ")", SubscriptionManager.DATA_ROAMING_ENABLE),
                RandomOptionInt.create("DISABLED (" + SubscriptionManager.DATA_ROAMING_DISABLE + ")", SubscriptionManager.DATA_ROAMING_DISABLE));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        randomOption(true).randomize(context);
    }
}