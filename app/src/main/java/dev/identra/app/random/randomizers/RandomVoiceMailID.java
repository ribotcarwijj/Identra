
package dev.identra.app.random.randomizers;

import androidx.annotation.NonNull;

import java.util.List;

import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.utilities.RandomStringGenerator;

public class RandomVoiceMailID implements IRandomizerOld {
    @Override
    public boolean isSetting(String setting) { return setting.equalsIgnoreCase(getSettingName()); }

    @Override
    public String getSettingName() {  return "unique.gsm.voicemail.id"; }

    @Override
    public String getName() {
        return "Voicemail Alpha Tag";
    }

    @Override
    public String getID() {
        return "%voicemail_alpha_tag%";
    }

    @Override
    public String generateString() { return RandomStringGenerator.generateRandomAlphanumericString(8, RandomStringGenerator.LOWER_LETTERS); }

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

