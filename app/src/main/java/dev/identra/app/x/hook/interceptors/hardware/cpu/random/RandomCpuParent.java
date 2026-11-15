package dev.identra.app.x.hook.interceptors.hardware.cpu.random;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.deprecated.SettingsContextOld;
import dev.identra.app.x.core.settings.random_old.interfaces.ILinkParent;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomCpuParent extends RandomElement implements ILinkParent {
    public static IRandomizer create() { return new RandomCpuParent(); }
    public RandomCpuParent() {
        super("CPU Control Parent");
        bindSetting("cpu.info.file");
        setIsParentControlOverride(true);
    }

    @Override
    public String generateString() {
        boolean is64 = RandomGenerator.nextBoolean();
        boolean isBig = RandomGenerator.nextBoolean();
        return isBig ? is64 ? "amd64" : "i686" : is64 ? "aarch64" : "armv71";
    }

    @Override
    public String randomize(SettingsContextOld context) {
        //return super.randomize(context);
        return null;
    }
}
