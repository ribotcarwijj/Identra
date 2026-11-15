package dev.identra.app.x.core.settings.random.randomizers;
//identra.ignore.default.values

import dev.identra.app.x.Str;
import dev.identra.app.x.core.settings.SettingHolder;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionBoolean;
import dev.identra.app.x.core.settings.random.RandomOptionNullElement;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;

public class RandomXPDefaultValue extends RandomElement {
    public RandomXPDefaultValue() {
        super("XP Reserved (Defaults)");
        putSettings(RandomizersCache.SETTING_XP_DEFAULTS);
        putOptions(RandomOptionBoolean.generate());

        //Str.bytesToHex()
        //new SettingHolder().getNa
    }

    //ListView of checkable items
    //Save to Settings
    //When intializing take that list put "ingore." in front of the name

    @Override
    public void randomize(RandomizerSessionContext context) {
        randomOption().randomize(context);
    }
}