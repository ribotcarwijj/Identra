package dev.identra.app.x.core.settings.random.randomizers.network;

import dev.identra.app.x.Str;
import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomNetDNSList extends RandomElement {
    public RandomNetDNSList() {
        super("Network DNS List");
        putSettings(RandomizersCache.SETTING_NET_DNS_LIST);
        putParents(RandomizersCache.SETTING_NET_PARENT_CONTROL);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), Str.joinList(new NetInfoGenerator().getDnsServers()));
    }
}