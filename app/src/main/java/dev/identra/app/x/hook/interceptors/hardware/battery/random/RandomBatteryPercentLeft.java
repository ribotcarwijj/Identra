package dev.identra.app.x.hook.interceptors.hardware.battery.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomBatteryPercentLeft extends RandomElement {
    public static IRandomizer create() { return new RandomBatteryPercentLeft(); }
    public RandomBatteryPercentLeft() {
        super("Battery Percentage Remaining");
        bindSetting("battery.percentage.left");
    }

    @Override
    public String generateString() { return String.valueOf(RandomGenerator.nextInt(1, 100)); }
}
