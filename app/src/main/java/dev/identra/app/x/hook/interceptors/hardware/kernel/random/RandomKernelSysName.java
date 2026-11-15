package dev.identra.app.x.hook.interceptors.hardware.kernel.random;

import dev.identra.app.utilities.RandomStringGenerator;
import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomKernelSysName extends RandomElement {
    public static IRandomizer create() { return new RandomKernelSysName(); }
    public static final String[] SYS_NAMES = new String[] {
            "Linux",
            "Unix",
            "BSD",
            "XNU",
            "random" };

    public RandomKernelSysName() {
        super("Kernel Sys Name (Linux, Unix...)");
        bindSetting("android.kernel.sys.name");
    }

    @Override
    public String generateString() {
        String nodeName = SYS_NAMES[RandomGenerator.nextInt(0, SYS_NAMES.length)];
        if ("random".equals(nodeName))
            return RandomStringGenerator.generateRandomAlphanumericString(RandomGenerator.nextInt(5, 15), RandomStringGenerator.LOWER_LETTERS);
        return nodeName;
    }
}