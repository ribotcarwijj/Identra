package dev.identra.app.x.ui.models;

import android.app.Application;

import dev.identra.app.x.ui.core.model.ListRepoViewModel;
import dev.identra.app.x.core.settings.SettingsContainer;

public class SettingsExContainerViewModel  extends ListRepoViewModel<SettingsContainer> {
    public SettingsExContainerViewModel(Application application) {
        super(application, "xlua_settings_ex_hooks");   //SettingsGroupRepository.INSTANCE
    }
}
