package dev.identra.app.x.core.settings.random.randomizers.cell;

import android.telephony.SubscriptionManager;

import java.util.List;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.hook.interceptors.cell.SubscriptionInfoUtils;
import dev.identra.app.x.runtime.reflect.DynField;
import dev.identra.app.x.runtime.reflect.ReflectUtil;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomOptionInt;
import dev.identra.app.x.core.settings.random.RandomOptionNullElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomCellDisplayNameSource extends RandomElement {

    public RandomCellDisplayNameSource() {
        super("CELL Display Name Source");
        putIndexSettings(RandomizersCache.SETTING_CELL_DISPLAY_NAME_SOURCE, 1, 2);
        putOptions(
                RandomOptionNullElement.create(),
                RandomOptionInt.create(SubscriptionInfoUtils.NAME_SOURCE_UNKNOWN_NAME, SubscriptionInfoUtils.NAME_SOURCE_UNKNOWN),
                RandomOptionInt.create(SubscriptionInfoUtils.NAME_SOURCE_CARRIER_ID_NAME, SubscriptionInfoUtils.NAME_SOURCE_CARRIER_ID),
                RandomOptionInt.create(SubscriptionInfoUtils.NAME_SOURCE_SIM_SPN_NAME, SubscriptionInfoUtils.NAME_SOURCE_SIM_SPN),
                RandomOptionInt.create(SubscriptionInfoUtils.NAME_SOURCE_USER_INPUT_NAME, SubscriptionInfoUtils.NAME_SOURCE_USER_INPUT),
                RandomOptionInt.create(SubscriptionInfoUtils.NAME_SOURCE_CARRIER_NAME, SubscriptionInfoUtils.NAME_SOURCE_CARRIER),
                RandomOptionInt.create(SubscriptionInfoUtils.NAME_SOURCE_SIM_PNN_NAME, SubscriptionInfoUtils.NAME_SOURCE_SIM_PNN));

        //putRequirementsAsIndex(RandomizersCache.SETTING_CELL_SIM_COUNTRY_CODE, RandomizersCache.SETTING_CELL_OPERATOR_MNC);
    }

    //return RandomStringGenerator.generateRandomNumberString(18) + RandomStringGenerator.generateRandomLetterString(1, "ABCDEF");
    @Override
    public void randomize(RandomizerSessionContext context) {
        /*String name = context.stack.pop();
        if(!context.wasRandomized(name)) {
            List<String> req = context.resolveRequirements(getRequirements(name));
            String countryCode = context.getValue(req.get(0));
            String mnc = context.getValue(req.get(1));
            context.pushSpecial(name, generateIccid("89", countryCode, mnc));
        }*/

        randomOption(true).randomize(context);
    }
}
