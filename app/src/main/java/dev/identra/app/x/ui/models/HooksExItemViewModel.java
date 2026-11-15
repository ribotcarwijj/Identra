package dev.identra.app.x.ui.models;

import android.app.Application;

import dev.identra.app.x.ui.adapters.hooks.HooksRepository;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.ui.core.model.ListRepoViewModel;
import dev.identra.app.x.core.repos.SettingsGroupRepository;
import dev.identra.app.x.core.settings.SettingsGroup;

public class HooksExItemViewModel extends ListRepoViewModel<XHook> {
    public HooksExItemViewModel(Application application) {
        super(application, "xlua_hooks_ex_hooks", HooksRepository.INSTANCE);
    }
}
