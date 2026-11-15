package dev.identra.app.x.network.randomizers;

import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomHostName extends RandomElement {
    public static RandomHostName create() { return new RandomHostName(); }
    public RandomHostName() {
        super("Network Host Name");
        bindSetting("network.host.name");
    }

    @Override
    public String generateString() { return new NetInfoGenerator().getIpv4Address(); }
}