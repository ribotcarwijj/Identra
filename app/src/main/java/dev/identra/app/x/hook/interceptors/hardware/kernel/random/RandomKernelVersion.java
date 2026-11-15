package dev.identra.app.x.hook.interceptors.hardware.kernel.random;


import dev.identra.app.x.hook.interceptors.zone.random.RandomDateZero;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomKernelVersion extends RandomElement {
    public static IRandomizer create() { return new RandomKernelVersion(); }
    public RandomKernelVersion() {
        super("Kernel Version");
        bindSetting("android.kernel.version");
    }

    @Override
    public String generateString() { return "SMP PREEMPT " + RandomDateZero.create().generateString(); }
}