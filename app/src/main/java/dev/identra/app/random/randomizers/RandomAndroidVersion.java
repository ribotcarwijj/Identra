package dev.identra.app.random.randomizers;

import androidx.annotation.NonNull;

import java.util.List;

import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.utilities.RandomUtil;

public class RandomAndroidVersion implements IRandomizerOld {

    @Override
    public boolean isSetting(String setting) { return setting.equalsIgnoreCase(getSettingName()); }

    @Override
    public String getSettingName() {  return "android.build.version"; }

    @Override
    public String getName() {
        return "Android Build Version";
    }

    @Override
    public String getID() {
        return "%android_build_version%";
    }

    @Override
    public String generateString() { return String.valueOf(RandomUtil.getInt(6, 15)); }

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
