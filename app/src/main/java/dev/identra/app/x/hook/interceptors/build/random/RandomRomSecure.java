package dev.identra.app.x.hook.interceptors.build.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomRomSecure extends RandomElement {
    public static IRandomizer create() { return new RandomRomSecure(); }
    public static final String[] SECURE_STATES = new String[] {
            "enforce",
            "log",
            "disable"
    };

    public RandomRomSecure() {
        super("Build ROM Secure");
        bindSetting("android.rom.secure");
    }

    @Override
    public String generateString() { return RandomGenerator.nextElement(SECURE_STATES);  }
}
