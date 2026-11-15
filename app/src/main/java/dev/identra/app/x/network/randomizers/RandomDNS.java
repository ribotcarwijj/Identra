package dev.identra.app.x.network.randomizers;

import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomDNS extends RandomElement {
    public static RandomDNS create() { return new RandomDNS(); }
    public RandomDNS() {
        super("Network DNS");
        bindSetting("network.dns");
    }

    @Override
    public String generateString() { return new NetInfoGenerator().getDnsServers().get(0); }
}
