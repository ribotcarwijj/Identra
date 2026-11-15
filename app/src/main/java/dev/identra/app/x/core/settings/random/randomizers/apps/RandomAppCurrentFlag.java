package dev.identra.app.x.core.settings.random.randomizers.apps;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionNullElement;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomAppCurrentFlag /*extends RandomElement*/ {
    /*public RandomAppCurrentFlag() {
        super("Apps Time Current App");
        putSettings(RandomizersCache.SETTING_APP_TIME_CURRENT_ONLY);
        putOptions(
                RandomOptionNullElement.create(),
                RandomOptionString.create("true"),
                RandomOptionString.create("false"));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        if(!context.stack.isEmpty())
            context.stack.pop();
    }*/
}