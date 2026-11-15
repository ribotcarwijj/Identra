package dev.identra.app.x.hook.interceptors.hardware.cpu.random;

import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomCpuManufacturer extends RandomElement {
    public static IRandomizer create() { return new RandomCpuManufacturer(); }
    public RandomCpuManufacturer() {
        super("CPU Manufacturer Name");
        bindSetting("cpu.soc.manufacturer");
    }

    @Override
    public String generateString() {
        int index = RandomGenerator.nextInt(0, RandomCpuHardwareName.MANUFACTURERS_TO_SHORT.size());
        return ListUtil.copyToList( RandomCpuHardwareName.MANUFACTURERS_TO_SHORT.values()).get(index);
    }
}
