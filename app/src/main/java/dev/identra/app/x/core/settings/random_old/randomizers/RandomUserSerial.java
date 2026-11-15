package dev.identra.app.x.core.settings.random_old.randomizers;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomUserSerial extends RandomElement {
    public static RandomUserSerial create() { return new RandomUserSerial(); }
    public RandomUserSerial() {
        super("User Profile Serial");
        bindSetting("account.user.serial");
    }

    @Override
    public String generateString() { return String.valueOf(RandomGenerator.nextInt(1000, 9999)); }
}