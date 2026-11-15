package dev.identra.app.random.randomizers;

import androidx.annotation.NonNull;

import java.util.List;

import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.utilities.RandomStringGenerator;
import dev.identra.app.x.data.utils.random.RandomGenerator;

public class RandomSerial implements IRandomizerOld {

    @Override
    public boolean isSetting(String setting) { return setting.equalsIgnoreCase(getSettingName()); }

    @Override
    public String getSettingName() {  return "unique.serial.no"; }

    @Override
    public String getName() {
        return "Serial Number";
    }

    @Override
    public String getID() {
        return "%serial%";
    }

    @Override
    public String generateString() { return RandomStringGenerator.generateRandomAlphanumericString(RandomGenerator.nextInt(8, 18), RandomStringGenerator.UPPER_LETTERS); }

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
