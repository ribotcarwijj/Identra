package dev.identra.app.x.core.settings.random_old.randomizers;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

//android.build.description
public class RandomGenericStringOne extends RandomElement {
    public static IRandomizer create() { return new RandomGenericStringOne(); }

    public RandomGenericStringOne() {
        super("Random String (1)");
        bindSettings(
                "android.build.description",
                "android.build.display.id",
                "android.build.fingerprint",
                "android.build.flavor",
                "android.build.host",
                "android.build.incremental",
                "clipboard.contents",
                "clipboard.label");
    }

    @Override
    public String generateString() { return RandomGenerator.generateRandomAlphanumericString(RandomGenerator.nextInt(6, 50));  }
}
