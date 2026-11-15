package dev.identra.app.x.core.settings.random.randomizers.network;

import java.util.List;

import dev.identra.app.x.Str;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomNetDNS extends RandomElement {
    public RandomNetDNS() {
        super("Network DNS");
        putSettings(RandomizersCache.SETTING_NET_DNS);
        putRequirement(RandomizersCache.SETTING_NET_DNS_LIST);
        putParents(RandomizersCache.SETTING_NET_PARENT_CONTROL);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        String name = context.stack.pop();
        if(!context.wasRandomized(name)) {
            List<String> req = context.resolveRequirements(getRequirements(name));
            if(ListUtil.isValid(req)) {
                String dnsList = context.getValue(req.get(0));
                List<String> list = Str.splitToList(dnsList);
                context.pushSpecial(name, list.get(0));
            }
        }
    }
}