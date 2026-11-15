package dev.identra.app.x.hook.interceptors.hardware.cpu.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomCpuProcessorCount extends RandomElement {
    public static IRandomizer create() { return new RandomCpuProcessorCount(); }
    public RandomCpuProcessorCount() {
        super("CPU Processor Count");
        bindSetting("cpu.processor.count");
    }

    @Override
    public String generateString() { return String.valueOf(RandomGenerator.nextInt(3, 22)); }
}
