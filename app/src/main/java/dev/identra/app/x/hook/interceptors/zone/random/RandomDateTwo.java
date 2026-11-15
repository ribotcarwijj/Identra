package dev.identra.app.x.hook.interceptors.zone.random;

import dev.identra.app.utilities.RandomUtil;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomDateTwo extends RandomElement {
    public static IRandomizer create() { return new RandomDateTwo(); }
    public static final String FORMAT = "%s%s%s";

    public RandomDateTwo() {
        super("Random Date Two (YYYYMMDD)");
        bindSetting("android.build.date.two");
    }

    @Override
    public String generateString() {
        return String.format(FORMAT,
                RandomUtil.getInt(1999, 2030),
                RandomUtil.getMonthNumberFormatted(),
                RandomUtil.getIntEnsureFormat(1, 30));
    }
}
