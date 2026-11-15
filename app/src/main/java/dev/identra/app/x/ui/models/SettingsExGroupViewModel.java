package dev.identra.app.x.ui.models;

import android.app.Application;

import dev.identra.app.x.data.PrefManager;
import dev.identra.app.x.ui.core.model.ListRepoViewModel;
import dev.identra.app.x.core.repos.SettingsGroupRepository;
import dev.identra.app.x.core.settings.SettingsGroup;

public class SettingsExGroupViewModel extends ListRepoViewModel<SettingsGroup> {
    public SettingsExGroupViewModel(Application application) {
        super(application, PrefManager.SETTINGS_NAMESPACE, SettingsGroupRepository.INSTANCE);
    }
}
