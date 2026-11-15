package dev.identra.app.x.network.randomizers;

import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomGateway extends RandomElement {
    public static RandomGateway create() { return new RandomGateway(); }
    public RandomGateway() {
        super("Network Gateway");
        bindSetting("network.gateway");
    }

    @Override
    public String generateString() { return new NetInfoGenerator().getGateway(); }
}