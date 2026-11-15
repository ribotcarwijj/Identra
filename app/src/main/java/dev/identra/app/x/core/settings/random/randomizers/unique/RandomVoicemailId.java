package dev.identra.app.x.core.settings.random.randomizers.unique;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomVoicemailId extends RandomElement {
    public RandomVoicemailId() {
        super("Unique Voicemail ID (alpha tag)");
        putIndexSettings(RandomizersCache.SETTING_UNIQUE_VOICEMAIL_ID, 1, 2);

    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(),
                RandomGenerator.nextBoolean() ?
                "Voice Mail" : RandomGenerator.nextString(8).toLowerCase());
    }
}