package dev.identra.app.x.network.randomizers;

import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomDomains extends RandomElement {
    public static RandomDomains create() { return new RandomDomains(); }
    public RandomDomains() {
        super("Network Domains");
        bindSetting("network.domains");
    }

    @Override
    public String generateString() { return new NetInfoGenerator().getDomain(); }
}