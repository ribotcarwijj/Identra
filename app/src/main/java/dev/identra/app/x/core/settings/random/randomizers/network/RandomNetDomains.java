package dev.identra.app.x.core.settings.random.randomizers.network;

import dev.identra.app.x.Str;
import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomNetDomains extends RandomElement {
    public RandomNetDomains() {
        super("Network Domains");
        putSettings(RandomizersCache.SETTING_NET_DOMAINS);
        putParents(RandomizersCache.SETTING_NET_PARENT_CONTROL);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), new NetInfoGenerator().getDomain());
    }
}