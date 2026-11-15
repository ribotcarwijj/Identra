package dev.identra.app.x.network.randomizers;

import dev.identra.app.x.Str;
import dev.identra.app.x.network.NetInfoGenerator;
import dev.identra.app.x.core.settings.random_old.RandomElement;

//network.routes
public class RandomRoutes extends RandomElement {
    public static RandomRoutes create() { return new RandomRoutes(); }
    public RandomRoutes() {
        super("Network Routes");
        bindSetting("network.routes");
    }

    @Override
    public String generateString() { return Str.joinList(new NetInfoGenerator().getRoutes(), ","); }
}