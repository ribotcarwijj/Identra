package dev.identra.app.x.hook.interceptors.build.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomBuildRadio extends RandomElement {
    public static IRandomizer create() { return new RandomBuildRadio(); }
    public RandomBuildRadio() {
        super("Build Radio Version");
        bindSetting("soc.baseband.board.radio.version");
    }

    @Override
    public String generateString() { return String.valueOf(RandomGenerator.nextInt(100, 999)); }
}