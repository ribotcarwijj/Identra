package dev.identra.app.x.core.settings.random.randomizers.cell;

import android.os.Build;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomOptionNullElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomPhoneType extends RandomElement {
    public RandomPhoneType() {
        super("CELL Phone Type");
        putIndexSettings(RandomizersCache.SETTING_PHONE_KIND, 1, 2);
        putOptions(
                RandomOptionNullElement.create(),
                RandomOptionInt.create("None", TelephonyManager.PHONE_TYPE_NONE),
                RandomOptionInt.create("GSM", TelephonyManager.PHONE_TYPE_GSM),
                RandomOptionInt.create("CDMA", TelephonyManager.PHONE_TYPE_CDMA),
                RandomOptionInt.create("SIP", TelephonyManager.PHONE_TYPE_SIP));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        randomOption(true).randomize(context);
    }
}