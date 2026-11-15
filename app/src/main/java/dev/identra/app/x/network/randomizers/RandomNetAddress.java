package dev.identra.app.x.network.randomizers;

import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomNetAddress extends RandomElement {
    public static RandomNetAddress create() { return new RandomNetAddress(); }
    public RandomNetAddress() {
        super("Network Host Address");
        bindSetting("network.host.address");
    }

    @Override
    public String generateString() { return new NetInfoGenerator().getIpv4Address(); }
}