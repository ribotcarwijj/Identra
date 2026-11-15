package dev.identra.app.x.ui.core.interfaces;

import dev.identra.app.x.data.PrefManager;
import dev.identra.app.x.ui.core.UserClientAppContext;
import dev.identra.app.x.core.repos.IIdentraRepo;

public interface IRepositoryContainer<TElement> {
    void setUserContext(UserClientAppContext userContext);
    void setRepository(IIdentraRepo<TElement> repository);

    UserClientAppContext getUserContext();
    IIdentraRepo<TElement> getRepository();
    PrefManager getPreferences();
}
