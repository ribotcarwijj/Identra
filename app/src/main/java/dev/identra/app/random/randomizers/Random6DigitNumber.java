package dev.identra.app.random.randomizers;

import androidx.annotation.NonNull;

import java.util.List;

import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.x.data.utils.random.RandomGenerator;

public class Random6DigitNumber implements IRandomizerOld {
    //"gsm.operator.mcc"
    @Override
    public boolean isSetting(String setting) { return getSettingName().equalsIgnoreCase(setting); }

    @Override
    public String getSettingName() { return "gsm.operator.id"; }

    @Override
    public String getName() {
        return "6 Digit Number";
    }

    @Override
    public String getID() {
        return "%six_digit_number%";
    }

    @Override
    public String generateString() { return Integer.toString(RandomGenerator.nextInt(100000, 999999)); }

    @Override
    public int generateInteger() { return 0; }

    @Override
    public List<ISpinnerElement> getOptions() { return null; }

    @NonNull
    @Override
    public String toString() { return getName(); }
}
