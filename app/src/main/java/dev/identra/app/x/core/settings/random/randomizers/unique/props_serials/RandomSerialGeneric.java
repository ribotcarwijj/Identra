package dev.identra.app.x.core.settings.random.randomizers.unique.props_serials;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.utils.SerialNumberGenerator;

public class RandomSerialGeneric extends RandomElement {
    public RandomSerialGeneric() {
        super("Unique Serial NO Generic");
        putSettings(
                "props.unique.ril.serialnumber",
                "props.unique.ap.serial",
                "props.unique.em.did",
                "props.unique.lite.uid",
                "props.unique.persist.radio.serialno");
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), SerialNumberGenerator.generateSerial());
    }
}