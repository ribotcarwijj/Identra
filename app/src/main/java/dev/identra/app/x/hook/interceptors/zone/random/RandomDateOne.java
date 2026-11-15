package dev.identra.app.x.hook.interceptors.zone.random;

import dev.identra.app.utilities.RandomUtil;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomDateOne extends RandomElement {
    public static IRandomizer create() { return new RandomDateOne(); }
    public static final String FORMAT = "%s.%s.%s";

    public RandomDateOne() {
        super("Random Date (YYYY.MM.DD)");
        bindSetting("android.build.date.one");
    }

    @Override
    public String generateString() {
        return String.format(FORMAT,
                RandomUtil.getInt(1999, 2030),
                RandomUtil.getMonthNumberFormatted(),
                RandomUtil.getIntEnsureFormat(1, 30));
    }
}
