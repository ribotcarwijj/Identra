package dev.identra.app.x.hook.interceptors.hardware.cpu.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomAbiList extends RandomElement {
    public static IRandomizer create() { return new RandomAbiList(); }
    public RandomAbiList() {
        super("CPU Abi List");
        bindSetting("cpu.abilist");
        bindParent("cpu.abi");
    }

    @Override
    public String generateString() {
        boolean is64 = RandomGenerator.nextBoolean();
        boolean isBig = RandomGenerator.nextBoolean();
        //return !isBig ? Str.joinList(ListUtil.combine(RandomCpuAbi.ABI_ARM_64, RandomCpuAbi.ABI_ARM, is64, is64), ",") :
        //        is64 ? "x86_64,x86" : "x86";
        return null;
    }
}
