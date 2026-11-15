package dev.identra.app.x.core.settings.random.randomizers.generic;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanAndUtils;

public class RandomDateEpocSeconds extends RandomElement {
    public RandomDateEpocSeconds() {
        super("Random Date EPOC");
        putSettings(RandomizersCache.SETTING_ANDROID_BUILD_DATE_EPOC);
        //putParents(RandomizersCache.SETTING_PARENT_DEVICE);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        for(String setting : getSettings()) {
            //Hmm Do better ToDO
            context.pushValue(setting, RanAndUtils.dateEpoch(context));
            //set the others ? either way the "others" will have to follow
            //Or the sub settings like build date can find its epoc brother
            //Then convert ?
        }
    }
}
