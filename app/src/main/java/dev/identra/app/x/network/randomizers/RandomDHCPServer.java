package dev.identra.app.x.network.randomizers;

import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomDHCPServer extends RandomElement {
    public static RandomDHCPServer create() { return new RandomDHCPServer(); }
    public RandomDHCPServer() {
        super("Network DHCP Server");
        bindSetting("network.dhcp.server");
    }

    @Override
    public String generateString() { return new NetInfoGenerator().getDhcpServer(); }
}