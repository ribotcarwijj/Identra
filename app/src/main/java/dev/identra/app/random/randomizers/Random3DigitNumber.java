package dev.identra.app.random.randomizers;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;


import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.x.data.utils.random.RandomGenerator;

public class Random3DigitNumber implements IRandomizerOld {
    private static final List<String> SETTINGS = Arrays.asList("gsm.operator.mnc", "gsm.operator.mcc", "android.build.radio");

    @Override
    public boolean isSetting(String setting) { return SETTINGS.contains(setting); }

    @Override
    public String getSettingName() {
        return "three.digit.number";
    }

    @Override
    public String getName() {
        return "3 Digit Number";
    }

    @Override
    public String getID() {
        return "%three_digit_number%";
    }

    @Override
    public String generateString() { return Integer.toString(RandomGenerator.nextInt(100, 999)); }

    @Override
    public int generateInteger() { return 0; }

    @Override
    public List<ISpinnerElement> getOptions() { return null; }

    @NonNull
    @Override
    public String toString() { return getName(); }
}
