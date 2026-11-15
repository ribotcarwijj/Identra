package dev.identra.app.x.network.randomizers;

import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomNetMask extends RandomElement {
    public static RandomNetMask create() { return new RandomNetMask(); }
    public RandomNetMask() {
        super("Network Net Mask");
        bindSetting("network.netmask");
    }

    @Override
    public String generateString() { return new NetInfoGenerator().getNetmask(); }
}