package dev.identra.app.x.hook.interceptors.build.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomBuildSDK extends RandomElement {
    public static IRandomizer create() { return new RandomBuildSDK(); }
    public RandomBuildSDK() {
        super("Build SDK");
        bindSettings("android.build.min.sdk", "android.build.version.sdk");
    }

    @Override
    public String generateString() { return String.valueOf(RandomGenerator.nextInt(23, 35)); }
}