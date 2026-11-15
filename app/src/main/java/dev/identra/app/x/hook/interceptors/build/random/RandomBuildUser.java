package dev.identra.app.x.hook.interceptors.build.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomBuildUser extends RandomElement {
    public static IRandomizer create() { return new RandomBuildUser(); }
    public static final String[] DEFAULT_BUILD_USERS = new String[] {
            "jenkins",
            "buildbot",
            "android-build",
            "ido",
            "god",
            "random"
    };

    public RandomBuildUser() {
        super("Build User");
        bindSetting("android.build.user");
    }

    @Override
    public String generateString() { return DEFAULT_BUILD_USERS[RandomGenerator.nextInt(0, DEFAULT_BUILD_USERS.length)]; }
}