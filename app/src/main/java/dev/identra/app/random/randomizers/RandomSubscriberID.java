
package dev.identra.app.random.randomizers;

import androidx.annotation.NonNull;

import java.util.List;

import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.utilities.RandomStringGenerator;

public class RandomSubscriberID implements IRandomizerOld {
    @Override
    public boolean isSetting(String setting) { return setting.equalsIgnoreCase(getSettingName()) || setting.equalsIgnoreCase("unique.gsm.subscription.id") ; }

    @Override
    public String getSettingName() {  return "phone.subscriberid"; }

    @Override
    public String getName() { return "Subscriber ID"; }

    @Override
    public String getID() { return "%subscriber_id%"; }

    @Override
    public String generateString() { return RandomStringGenerator.generateRandomNumberString(10); }

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
