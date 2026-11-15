package dev.identra.app.random.randomizers;

import androidx.annotation.NonNull;

import java.util.List;

import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.utilities.RandomStringGenerator;

public class RandomGSF implements IRandomizerOld {
    @Override
    public boolean isSetting(String setting) {
        return setting.equalsIgnoreCase(getSettingName()) || setting.equalsIgnoreCase("unique.gsf.id");
    }

    @Override
    public String getSettingName() { return "gsf.id"; }

    @Override
    public String getName() {
        return "GSF ID";
    }

    @Override
    public String getID() {
        return "%gsf_id%";
    }

    @Override
    public String generateString() {
        int length = 16; //or 32
        //String characters = "0123456789ABCDEF";
        //StringBuilder result = new StringBuilder(length);
        //Random random = new Random();
        //for (int i = 0; i < length; i++) result.append(characters.charAt(random.nextInt(characters.length())));
        //return result.toString();
        return RandomStringGenerator.generateRandomNumberString(length);
    }

    @Override
    public int generateInteger() { return 0; }

    @Override
    public List<ISpinnerElement> getOptions() { return null; }

    @NonNull
    @Override
    public String toString() { return getName(); }
}
