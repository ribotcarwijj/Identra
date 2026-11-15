package dev.identra.app.x.core.settings.random.randomizers.hardware.gpu;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.randomizers.hardware.gpu.ran.RanGpuUtils;

public class RandomSocBluetoothBoardConfigName extends RandomElement {
    public RandomSocBluetoothBoardConfigName() {
        super("Hardware Bluetooth Board Config Name");
        putSettings(RandomizersCache.SETTING_SOC_BLUETOOTH_BOARD_CONFIG_NAME);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), RanGpuUtils.socBasebandBoardRadioVersion(context));
    }
}