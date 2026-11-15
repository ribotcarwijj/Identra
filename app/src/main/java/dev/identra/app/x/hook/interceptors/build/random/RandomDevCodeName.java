package dev.identra.app.x.hook.interceptors.build.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomDevCodeName extends RandomElement {
    public static IRandomizer create() { return new RandomDevCodeName(); }

    public RandomDevCodeName() {
        super("Build DEV Code Name");
        bindSetting("android.build.dev.codename");
        setIsCheckableRandom(false);
    }

    @Override
    public String generateString() { return RandomGenerator.randomStringIfRandomElse(RandomBuildCodeName.DEV_CODE_NAMES[RandomGenerator.nextInt(0, RandomBuildCodeName.DEV_CODE_NAMES.length)]); }
}
