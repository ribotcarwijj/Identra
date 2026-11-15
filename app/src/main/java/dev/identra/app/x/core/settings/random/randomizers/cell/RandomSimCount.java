package dev.identra.app.x.core.settings.random.randomizers.cell;

import android.telephony.TelephonyManager;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomOptionNullElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomSimCount extends RandomElement {
    public RandomSimCount() {
        super("CELL SIM Count");
        putSettings(RandomizersCache.SETTING_CELL_SIM_COUNT);
        putOptions(RandomOptionInt.generateOptions(0, 1, 2));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        randomOption(true).randomize(context);
    }
}