package dev.identra.app.x.hook.interceptors.hardware.location.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.hook.interceptors.zone.RandomDateHelper;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomGpsModelYear extends RandomElement {
    public static RandomGpsModelYear create() { return new RandomGpsModelYear(); }
    public RandomGpsModelYear() {
        super("GPS Hardware Model Year");
        bindSetting("hardware.gps.model.year");
    }

    @Override
    public String generateString() { return String.valueOf(RandomGenerator.nextInt(2018, RandomDateHelper.CURRENT_YEAR)); }
}