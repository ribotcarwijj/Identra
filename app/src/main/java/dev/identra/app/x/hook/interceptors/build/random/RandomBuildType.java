package dev.identra.app.x.hook.interceptors.build.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomBuildType extends RandomElement {
    public static IRandomizer create() { return new RandomBuildType(); }
    public static final String[] DEFAULT_BUILD_TYPES = new String[] {
            "user",
            "userdebug",
            "eng"
    };


    public RandomBuildType() {
        super("Build Type");
        bindSetting("android.build.type");
    }

    @Override
    public String generateString() { return DEFAULT_BUILD_TYPES[RandomGenerator.nextInt(0, DEFAULT_BUILD_TYPES.length)]; }
}