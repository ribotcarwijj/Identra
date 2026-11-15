package dev.identra.app.x.core.settings.random.randomizers.cell._deprecated;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomMNC extends RandomElement {
    public static RandomMCC create() { return new RandomMCC(); }
    public RandomMNC() {
        super("Cell MNC");
        bindSetting(RandomizersCache.SETTING_CELL_OPERATOR_MNC);
    }

    @Override
    public String generateString() { return String.valueOf(RandomGenerator.nextInt(100, 999)); }
}
