package dev.identra.app.x.core.settings.random.randomizers.network;

import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomNetHostAddress extends RandomElement {
    public RandomNetHostAddress() {
        super("Network Host Address");
        putSettings(RandomizersCache.SETTING_NET_HOST);
        putParents(RandomizersCache.SETTING_NET_PARENT_CONTROL);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), new NetInfoGenerator().getIpv4Address());
    }
}