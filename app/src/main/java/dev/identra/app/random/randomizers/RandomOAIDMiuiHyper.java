package dev.identra.app.random.randomizers;


//isMiuiOrHyperOs


import androidx.annotation.NonNull;

import java.util.List;

import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.utilities.RandomStringGenerator;
import dev.identra.app.x.runtime.BuildInfo;

public class RandomOAIDMiuiHyper implements IRandomizerOld {
    @Override
    public boolean isSetting(String setting) { return getSettingName().equalsIgnoreCase(setting) && BuildInfo.isMiuiOrHyperOs(); }

    @Override
    public String getSettingName() { return "unique.open.anon.advertising.id"; }

    @Override
    public String getName() { return "OAID Miui/Hyper"; }

    @Override
    public String getID() { return "%oaid_miui_hyper%"; }

    @Override
    public String generateString() { return RandomStringGenerator.generateRandomHexString(16).toLowerCase(); }

    @Override
    public int generateInteger() { return 0; }

    @Override
    public List<ISpinnerElement> getOptions() { return null; }

    @NonNull
    @Override
    public String toString() { return getName(); }
}
