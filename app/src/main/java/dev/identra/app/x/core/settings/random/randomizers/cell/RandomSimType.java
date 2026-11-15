package dev.identra.app.x.core.settings.random.randomizers.cell;

import android.os.Build;
import android.telephony.SubscriptionManager;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomOptionNullElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomSimType extends RandomElement {
    public RandomSimType() {
        super("SIM Type");
        putIndexSettings(RandomizersCache.SETTING_SIM_KIND, 1, 2);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            putOptions(
                    RandomOptionNullElement.create(),
                    RandomOptionInt.create("Local SIM", SubscriptionManager.SUBSCRIPTION_TYPE_LOCAL_SIM),
                    RandomOptionInt.create("Remote SIM (E-SIM) ", SubscriptionManager.SUBSCRIPTION_TYPE_REMOTE_SIM));
        } else {
            putOptions(
                    RandomOptionNullElement.create(),
                    RandomOptionInt.create("Local SIM", 0),
                    RandomOptionInt.create("Remote SIM (E-SIM)", 1));
        }
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        randomOption(true).randomize(context);
    }
}