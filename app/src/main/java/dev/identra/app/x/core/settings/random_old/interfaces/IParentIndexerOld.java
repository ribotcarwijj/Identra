package dev.identra.app.x.core.settings.random_old.interfaces;

import java.util.List;

import dev.identra.app.x.core.settings.deprecated.SettingExtendedOld;

public interface IParentIndexerOld {
    boolean hasChildSettings();
    String getCategory();
    List<SettingExtendedOld> getChildSettings();
}
