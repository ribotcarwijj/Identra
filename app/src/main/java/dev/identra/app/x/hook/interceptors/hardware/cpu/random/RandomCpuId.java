package dev.identra.app.x.hook.interceptors.hardware.cpu.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomCpuId extends RandomElement {
    public static IRandomizer create() { return new RandomCpuId(); }
    public RandomCpuId() {
        super("CPU ID");
        bindSetting("cpu.cpuid");
    }

    @Override
    public String generateString() { return "0x" + RandomGenerator.generateRandomNumberString(RandomGenerator.nextInt(7, 18)); }
}
