package dev.identra.app.x.core.settings.random.randomizers.network;

import dev.identra.app.x.core.hook.PackageHookContext;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomNullElement;
import dev.identra.app.x.core.settings.random.RandomOptionNullElement;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomNetAllowedList extends RandomElement {
    public RandomNetAllowedList() {
        super("Network Show List");
        putSettings(RandomizersCache.SETTING_NET_ALLOWED_LIST);
        putOptions(RandomNullElement.create(),
                RandomOptionString.create("Random", PackageHookContext.RANDOM_VALUE));
        //putOptions(RandomOptionNullElement.create(),
               // RandomOptionString.create("Green Safe", "1"),
               // RandomOptionString.create("Red Blown", "0"));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}