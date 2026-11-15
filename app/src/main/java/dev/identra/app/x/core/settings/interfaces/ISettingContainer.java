package dev.identra.app.x.core.settings.interfaces;

import java.util.List;

import dev.identra.app.x.core.settings.SettingHolder;

public interface ISettingContainer {
    String getGroupName();
    String getContainerName();
    List<SettingHolder> getSettings();
    void ensureHasDescription(String description);
}
