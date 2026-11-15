package dev.identra.app.x.network.randomizers;

import dev.identra.app.x.Str;
import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomDNSList extends RandomElement {
    public static RandomDNSList create() { return new RandomDNSList(); }
    public RandomDNSList() {
        super("Network DNS List");
        bindSetting("network.dns.list");
    }

    @Override
    public String generateString() { return Str.joinList(new NetInfoGenerator().getDnsServers(), ","); }
}