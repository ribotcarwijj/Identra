package dev.identra.app.x.core.settings.random.randomizers.unique;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.randomizers.hardware.gpu.ran.RanGpuUtils;
import dev.identra.app.x.core.settings.random.utils.RanUnqUtils;

public class RandomBluetoothAddress extends RandomElement {
    public RandomBluetoothAddress() {
        super("Bluetooth Address");
        putSettings(RandomizersCache.SETTING_UNIQUE_BLUETOOTH);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RanUnqUtils.bluetoothAddress(context));
    }
}