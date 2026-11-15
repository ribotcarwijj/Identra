package dev.identra.app.x.hook.interceptors.build.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomBuildVersion extends RandomElement {
    public static IRandomizer create() { return new RandomBuildVersion(); }
    public RandomBuildVersion() {
        super("Build Android Version");
        bindSetting("android.build.version");
    }

    @Override
    public String generateString() { return String.valueOf(RandomGenerator.nextInt(6, 15)); }
}