package dev.identra.app.random.randomizers;

import androidx.annotation.NonNull;

import java.util.List;

import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.utilities.RandomUtil;

public class RandomDateEpoch implements IRandomizerOld {
    private static final String FORMAT = "%s-%s-%s %s:%s:%s";

    @Override
    public boolean isSetting(String setting) { return setting.equalsIgnoreCase(getSettingName()) || "android.build.date.utc".equalsIgnoreCase(setting); }

    @Override
    public String getSettingName() {  return "random.date.epoch"; }

    @Override
    public String getName() {
        return "Random Date Unix TimeStamp";
    }

    @Override
    public String getID() { return "%random_date_epoch%"; }

    @Override
    public String generateString() {
        return String.valueOf(RandomUtil.convertStringDateToEpoch(
                String.format(FORMAT,
                        RandomUtil.getIntEnsureFormat(1, 30),
                        RandomUtil.getMonthNumberFormatted(),
                        RandomUtil.getInt(1999, 2030),
                        RandomUtil.getIntEnsureFormat(1, 24),
                        RandomUtil.getIntEnsureFormat(1, 60),
                        RandomUtil.getIntEnsureFormat(1, 60)), RandomUtil.DD_MM_YYYY__HHMMSS));
    }

    @Override
    public int generateInteger() { return 0; }

    @Override
    public List<ISpinnerElement> getOptions() { return null; }

    @NonNull
    @Override
    public String toString() {
        return getName();
    }
}
