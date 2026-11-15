package dev.identra.app.random;

import java.util.List;

import dev.identra.app.random.elements.ISpinnerElement;

public interface IRandomizerOld {
    boolean isSetting(String settingName);
    String getSettingName();
    String getName();
    String getID();
    String generateString();
    int generateInteger();

    List<ISpinnerElement> getOptions();
    //byte[] generateBytes();
    //9177334325 jonny
}
