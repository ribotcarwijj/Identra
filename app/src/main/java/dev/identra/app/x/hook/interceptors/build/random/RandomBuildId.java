package dev.identra.app.x.hook.interceptors.build.random;

import dev.identra.app.utilities.RandomStringGenerator;
import dev.identra.app.utilities.RandomUtil;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomBuildId extends RandomElement {
    public static final String FORMAT = "%s.%s.%s";
    public static IRandomizer create() { return new RandomBuildId(); }

    public RandomBuildId() {
        super("Build ID");
        bindSetting("android.build.description");
    }


    @Override
    public String generateString() {
        return String.format(FORMAT,
                RandomStringGenerator.generateRandomLetterString(RandomUtil.getInt(4, 8), RandomStringGenerator.UPPER_LETTERS),
                RandomUtil.getInt(10000, 99999),
                RandomUtil.getIntEnsureFormat(1, 999));
    }
}
