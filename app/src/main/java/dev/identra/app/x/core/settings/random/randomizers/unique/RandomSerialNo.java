package dev.identra.app.x.core.settings.random.randomizers.unique;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.runtime.BuildInfo;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.SerialNumberGenerator;

public class RandomSerialNo extends RandomElement {
    public RandomSerialNo() {
        //No relation to ICC ID, it links to the prop ? gsm.serial
        //djhwlmt51pd30ar4ymtk
        //Cant find much on this
        //https://newandroidbook.com/ddb/SonyXperiaXA1UltraDual/getprop.txt
        //https://github.com/erleizh/build.prop/blob/master/OPPO%20R11.txt
        //super("CELL GSM Band Serial");
        super("Serial NO");
        //putIndexSettings(RandomizersCache.SETTING_UNIQUE_SIM_SERIAL, 1, 2);
        //putIndexSettings(RandomizersCache.SETTING_UNIQUE_SERIAL_NO, 1, 2);
        //RandomizersCache.SETTING_UNIQUE_SERIAL_NO
        putSettings(RandomizersCache.SETTING_UNIQUE_SERIAL_NO);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        if(BuildInfo.isMiuiOrHyperOs()) {
            context.pushSpecial(context.stack.pop(), SerialNumberGenerator.generateSerial(SerialNumberGenerator.SerialStyle.XIAOMI));
        } else {
            context.pushSpecial(context.stack.pop(), SerialNumberGenerator.generateSerial());
        }
    }
}
