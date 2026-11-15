package dev.identra.app.x.core.settings.random.randomizers.unique;

import java.util.UUID;

import dev.identra.app.utilities.RandomStringGenerator;
import dev.identra.app.x.runtime.BuildInfo;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomUUID extends RandomElement {
    public RandomUUID() {
        super("Random UUID");
        putSettings(
                RandomizersCache.SETTING_UNIQUE_UUID,
                RandomizersCache.SETTING_UNIQUE_VA_ID,
                RandomizersCache.SETTING_UNIQUE_ANON_ID,
                RandomizersCache.SETTING_UNIQUE_OPEN_ANON_ID,
                RandomizersCache.SETTING_UNIQUE_BOOT_ID,
                RandomizersCache.SETTING_UNIQUE_FACEBOOK_ID,
                RandomizersCache.SETTING_UNIQUE_GOOGLE_ID,
                RandomizersCache.SETTING_UNIQUE_GOOGLE_APP_SET_ID,
                RandomizersCache.SETTING_XI_MI_GC_BOOSTER_UUID,
                RandomizersCache.SETTING_XI_MI_KEY_MQS_UUID,
                RandomizersCache.SETTING_XI_MI_MDM_UUID,
                RandomizersCache.SETTING_OP_SEC_UUID,
                RandomizersCache.SETTING_XI_MI_EXTM_UUID,
                RandomizersCache.SETTING_UNIQUE_PAY_SESSION_ID);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        String setting = context.stack.pop();
        if(setting == null)
            return;

        if(BuildInfo.isMiuiOrHyperOs()) {
            if(RandomizersCache.SETTING_UNIQUE_OPEN_ANON_ID.equalsIgnoreCase(setting) ||
                    RandomizersCache.SETTING_UNIQUE_VA_ID.equalsIgnoreCase(setting)) {
                context.pushValue(setting, RandomStringGenerator.generateRandomHexString(16).toLowerCase());
                return;
            }
        }

        context.pushValue(setting, UUID.randomUUID().toString());
    }
}