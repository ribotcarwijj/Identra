package dev.identra.app.x.hook.interceptors.network.map;

import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.randomizers.RandomMAC;
import dev.identra.app.x.data.GroupedMap;

public class GroupedNetworkMap {
    private static final IRandomizerOld MAC_RANDOM = new RandomMAC();
    private final GroupedMap mGroupedMap;
    public GroupedNetworkMap(GroupedMap map) {
        mGroupedMap = map;
    }

    public String getMacAddress(String networkInterface) {
        return "";
    }
}
