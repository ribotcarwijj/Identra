package dev.identra.app.x.core.settings.random.randomizers.battery;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomBatteryPercent extends RandomElement {
    public RandomBatteryPercent() {
        super("Battery Charge (%)");
        putSettings(RandomizersCache.SETTING_BATTERY_PERCENT);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushSpecial(context.stack.pop(), String.valueOf(RandomGenerator.nextInt(1, 100)));
    }
}