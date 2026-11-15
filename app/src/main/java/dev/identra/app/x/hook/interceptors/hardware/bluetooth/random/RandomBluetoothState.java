package dev.identra.app.x.hook.interceptors.hardware.bluetooth.random;

import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;
import dev.identra.app.x.core.settings.random_old.extra.IndexedOptionElement;

public class RandomBluetoothState extends RandomElement {
    public static IRandomizer create() { return new RandomBluetoothState(); }
    public RandomBluetoothState() {
        super("Bluetooth State");
        bindSetting("bluetooth.state");
        bindOptions(
                IndexedOptionElement.create("STATE_OFF", 10),
                IndexedOptionElement.create("STATE_TURNING_ON", 11),
                IndexedOptionElement.create("STATE_ON", 12),
                IndexedOptionElement.create("STATE_TURNING_OFF", 12));
    }
}
