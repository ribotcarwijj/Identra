package dev.identra.app.random.randomizers;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;


import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.x.data.utils.random.RandomGenerator;

public class RandomCarrierName implements IRandomizerOld {
    private static final List<String> CARRIERS = Arrays.asList("Siminn", "Verizon", "ATT", "T-Mobile", "Mint Mobile", "Sprint", "Xfinity Mobile", "MetroPCS", "Boost Mobile", "US Mobile", "Beast Mobile", "Spectrum", "Cricket", "Google Fi", "Black Wireless");

    @Override
    public boolean isSetting(String setting) {
        return setting.equalsIgnoreCase(getSettingName());
    }

    @Override
    public String getSettingName() {  return "gsm.network.carrier"; }

    @Override
    public String getName() {
        return "Carrier";
    }

    @Override
    public String getID() {
        return "%net_carrier%";
    }
    @Override
    public String generateString() {
        return CARRIERS.get(RandomGenerator.nextInt(0, CARRIERS.size()));
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
