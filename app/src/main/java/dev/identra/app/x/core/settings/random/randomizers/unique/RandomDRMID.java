package dev.identra.app.x.core.settings.random.randomizers.unique;

import java.util.Random;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.RanUnqUtils;

public class RandomDRMID extends RandomElement {
    public RandomDRMID() {
        super("Unique DRM ID");
        putSettings(RandomizersCache.SETTING_UNIQUE_DRM_ID);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        // Generate a random alphanumeric string of length 32 (since we are working with 16 bytes)
        int length = 64; //or 32
        String characters = "0123456789ABCDEF";
        StringBuilder result = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++)
            result.append(characters.charAt(random.nextInt(characters.length())));

        context.pushValue(context.stack.pop(), result.toString());
    }
}