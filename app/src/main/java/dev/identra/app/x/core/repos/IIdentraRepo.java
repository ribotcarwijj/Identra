package dev.identra.app.x.core.repos;

import android.content.Context;

import java.util.List;

import dev.identra.app.x.ui.core.FilterRequest;
import dev.identra.app.x.ui.core.UserClientAppContext;

public interface IIdentraRepo<TElement> {
    List<TElement> get();
    List<TElement> get(Context context, UserClientAppContext userContext);
    List<TElement> filterAndSort(List<TElement> items, FilterRequest request);
}
