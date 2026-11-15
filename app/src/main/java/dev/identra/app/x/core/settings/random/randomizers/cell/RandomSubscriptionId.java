package dev.identra.app.x.core.settings.random.randomizers.cell;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomSubscriptionId extends RandomElement {
    public RandomSubscriptionId() {
        super("Subscription ID");
        putIndexSettings(RandomizersCache.SETTING_SIM_SUBSCRIPTION_ID, 1, 2);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        String setting = context.stack.pop();
        if(setting != null) {
            //Do Not Spoof this !
            if(setting.endsWith(".1")) {
                context.pushSpecial(
                        setting,
                        String.valueOf(RandomGenerator.nextInt(1, 5)));
            } else {
                context.pushSpecial(
                        setting,
                        String.valueOf(RandomGenerator.nextInt(6, 15)));
            }
        }
    }
}
