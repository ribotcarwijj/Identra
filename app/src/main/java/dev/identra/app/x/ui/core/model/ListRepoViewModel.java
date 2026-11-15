package dev.identra.app.x.ui.core.model;

import android.app.Application;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dev.identra.app.x.data.PrefManager;
import dev.identra.app.x.runtime.RuntimeUtils;
import dev.identra.app.x.ui.core.FilterRequest;
import dev.identra.app.x.ui.core.interfaces.IRepositoryContainer;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.repos.IIdentraRepo;
import kotlin.Pair;
import kotlin.Triple;

public class ListRepoViewModel<TElement> extends ListPreferenceViewModel<TElement> implements IRepositoryContainer<TElement> {

    protected IIdentraRepo<TElement> repository;

    private static final String TAG = LibUtil.generateTag(ListRepoViewModel.class);

    public ListRepoViewModel(Application application, String tag) {
        super(application, tag);
    }

    public ListRepoViewModel(Application application, String tag, IIdentraRepo<TElement> repository) {
        super(application, tag);
        this.repository = repository;
    }

    @Override
    protected List<TElement> filterData(Triple<Triple<String, String, List<String>>, Pair<String, Boolean>, Long> params, Application application) {
        if(!hasContext() || repository == null)
            return new ArrayList<>();

        Triple<String, String, List<String>> filter = params.getFirst();
        Log.d(TAG, "SHOW SHOW=" + filter.getSecond() + " Stack=" + RuntimeUtils.getStackTraceSafeString(new Exception()));
        Pair<String, Boolean> searchParams = params.getSecond();
        return repository.filterAndSort(
                repository.get(application, getUserContext()),
                FilterRequest
                    .create()
                    .setQuery(searchParams.getFirst())          //Query Item to Search For
                    .setIsReversed(searchParams.getSecond())    //Is Reversed Flags
                    .setOrder(filter.getFirst())                //Order Items by ...
                    .setShow(filter.getSecond())
                    .setFilterTags(filter.getThird()));        //List of the Tags for a more of a Filter, like Chips
    }

    @Override
    public void setRepository(IIdentraRepo<TElement> repository) {
        this.repository = repository;
    }

    @Override
    public IIdentraRepo<TElement> getRepository() {
        return this.repository;
    }

    @Override
    public PrefManager getPreferences() {
        return this.getPreferencesManger();
    }
}
