package dev.identra.app.random.randomizers;

import androidx.annotation.NonNull;

import java.util.List;

import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.utilities.RandomStringGenerator;

public class RandomMSIN implements IRandomizerOld {
    @Override
    public boolean isSetting(String setting) {
        return  setting.equalsIgnoreCase("gsm.operator.msin") || setting.equalsIgnoreCase(getSettingName());
    }

    @Override
    public String getSettingName() {  return "unique.gsm.operator.msin"; }

    @Override
    public String getName() {
        return "MSIN";
    }

    @Override
    public String getID() {
        return "%msin%";
    }

    @Override
    public String generateString() {
        return RandomStringGenerator.generateRandomNumberString(10);
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
