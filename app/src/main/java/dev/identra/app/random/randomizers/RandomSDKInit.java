package dev.identra.app.random.randomizers;

import androidx.annotation.NonNull;

import java.util.List;

import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.utilities.RandomUtil;

public class RandomSDKInit implements IRandomizerOld {
    @Override
    public boolean isSetting(String setting) { return setting.equalsIgnoreCase(getSettingName()) || "android.build.version.sdk".equalsIgnoreCase(setting); }

    @Override
    public String getSettingName() {  return "android.build.version.min.sdk"; }

    @Override
    public String getName() { return "Build SDK"; }

    @Override
    public String getID() { return "%build_sdk%"; }

    @Override
    public String generateString() { return String.valueOf(RandomUtil.getInt(23, 35  )); }

    @Override
    public int generateInteger() { return 0; }

    @Override
    public List<ISpinnerElement> getOptions() { return null; }

    @NonNull
    @Override
    public String toString() { return getName(); }
}
