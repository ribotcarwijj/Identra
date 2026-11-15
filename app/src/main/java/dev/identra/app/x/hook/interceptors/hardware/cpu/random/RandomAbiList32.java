package dev.identra.app.x.hook.interceptors.hardware.cpu.random;

import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomAbiList32 extends RandomElement {
    public static IRandomizer create() { return new RandomAbiList32(); }
    public RandomAbiList32() {
        super("CPU Abi List 32");
        bindSetting("cpu.abilist32");
        bindParent("cpu.abi");
    }

    @Override
    public String generateString() {
        //return RandomGenerator.nextBoolean() ? Str.joinList(RandomCpuAbi.ABI_ARM) : "x86";
        return null;
    }
}
