package dev.identra.app.x.core.settings.random.randomizers.battery;

import android.os.BatteryManager;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomNullElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomOptionNullElement;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomBatteryStatus extends RandomElement {
    public RandomBatteryStatus() {
        super("Battery Status");
        putSettings(RandomizersCache.SETTING_BATTERY_STATUS);
        putOptions(
                RandomOptionNullElement.create(),
                RandomOptionInt.create("Charging", BatteryManager.BATTERY_STATUS_CHARGING),
                RandomOptionInt.create("Discharging", BatteryManager.BATTERY_STATUS_DISCHARGING),
                RandomOptionInt.create("Full", BatteryManager.BATTERY_STATUS_FULL),
                RandomOptionInt.create("Not Charging", BatteryManager.BATTERY_STATUS_NOT_CHARGING),
                RandomOptionInt.create("Unknown", BatteryManager.BATTERY_STATUS_UNKNOWN));

    }

    @Override
    public void randomize(RandomizerSessionContext context) { randomOption().randomize(context); }
}