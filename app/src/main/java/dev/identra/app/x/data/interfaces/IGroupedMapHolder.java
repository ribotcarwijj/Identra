package dev.identra.app.x.data.interfaces;

import dev.identra.app.x.data.GroupedMap;

public interface IGroupedMapHolder {
    GroupedMap getGroupedMap();
    void setGroupedMap(GroupedMap map);
    boolean hasMap();
}

