package dev.identra.app.x.core.settings.random.randomizers.unique;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomGSFID extends RandomElement {
    public RandomGSFID() {
        super("Google Services Framework ID");
        putSettings(RandomizersCache.SETTING_UNIQUE_GSF_ID);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //Was 16 Char Length
        String setting = context.stack.pop();
        if(setting == null)
            return;

        context.pushSpecial(setting, RandomGenerator.nextStringNumeric(19));
    }
}