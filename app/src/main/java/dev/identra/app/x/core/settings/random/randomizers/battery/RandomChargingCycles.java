package dev.identra.app.x.core.settings.random.randomizers.battery;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

//battery.charging.cycles
public class RandomChargingCycles extends RandomElement {
    public RandomChargingCycles() {
        super("Battery Charging Cycles");
        putSettings(RandomizersCache.SETTING_BATTERY_CHARGING_CYCLES);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushSpecial(context.stack.pop(), String.valueOf(RandomGenerator.nextInt(10, 9999)));
    }
}