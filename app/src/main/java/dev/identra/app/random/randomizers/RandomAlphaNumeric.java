package dev.identra.app.random.randomizers;

import androidx.annotation.NonNull;

import java.util.List;


import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.utilities.RandomStringGenerator;
import dev.identra.app.x.data.utils.random.RandomGenerator;

public class RandomAlphaNumeric implements IRandomizerOld {
    @Override
    public boolean isSetting(String setting) {
        return setting.equalsIgnoreCase("gsm.setting.display.name");
    }

    @Override
    public String getSettingName() {
        return "alpha.str";
    }

    @Override
    public String getName() {
        return "Alphanumeric String";
    }

    @Override
    public String getID() {
        return "%alpha_string%";
    }

    @Override
    public String generateString() { return RandomStringGenerator.generateRandomAlphanumericString(RandomGenerator.nextInt(7, 18)); }

    @Override
    public int generateInteger() { return 0; }

    @Override
    public List<ISpinnerElement> getOptions() { return null; }

    @NonNull
    @Override
    public String toString() { return getName(); }
}
