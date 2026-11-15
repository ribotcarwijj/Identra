package dev.identra.app.x.core.settings.random.randomizers.network;

import dev.identra.app.x.Str;
import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomNetRoutes extends RandomElement {
    public RandomNetRoutes() {
        super("Network Routes");
        putSettings(RandomizersCache.SETTING_NET_ROUTES);
        putParents(RandomizersCache.SETTING_NET_PARENT_CONTROL);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), Str.joinList(new NetInfoGenerator().getRoutes()));
    }
}