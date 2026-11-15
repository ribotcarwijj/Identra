package dev.identra.app.x.hook.interceptors.hardware.battery.random;

import dev.identra.app.x.hook.interceptors.zone.RandomDateHelper;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomBatteryTimeRemaining extends RandomElement {
    public static IRandomizer create() { return new RandomBatteryTimeRemaining(); }
    public RandomBatteryTimeRemaining() {
        super("Battery Time Remaining (MS)");
        bindSetting("battery.charge.time.remaining");
    }

    @Override
    public String generateString() { return String.valueOf(RandomDateHelper.generateHoursInMilliseconds(2, 23)); }
}
