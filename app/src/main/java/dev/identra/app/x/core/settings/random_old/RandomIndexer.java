package dev.identra.app.x.core.settings.random_old;

import java.util.List;
import java.util.Map;

import dev.identra.app.x.core.settings.deprecated.IndexCache;
import dev.identra.app.x.core.settings.deprecated.SettingExtendedOld;
import dev.identra.app.x.core.settings.deprecated.SettingsHelperOld;
import dev.identra.app.x.core.settings.random_old.extra.IndexedElement;
import dev.identra.app.x.core.settings.random_old.interfaces.IParentIndexerOld;

public class RandomIndexer extends RandomElement implements IParentIndexerOld {
    private final String mCategory;
    private final List<SettingExtendedOld> mChildSettings;

    public static RandomIndexer create(String category, List<SettingExtendedOld> childSettings) { return new RandomIndexer(category, childSettings); }

    public RandomIndexer(String category, List<SettingExtendedOld> childSettings) {
        super(category + " Index Control");
        this.mChildSettings = childSettings;
        this.mCategory = category;
        SettingExtendedOld firstSettingForIndex = childSettings.get(0);
        Map<Integer, IndexCache> caches = SettingsHelperOld.getFactoryIndexCaches(firstSettingForIndex);
        if(caches != null) {
            for(Map.Entry<Integer, IndexCache> e : caches.entrySet()) {
                bindOption(new IndexedElement(e.getKey()));
            }
        }
    }

    @Override
    public boolean hasChildSettings() { return mChildSettings != null && !mChildSettings.isEmpty(); }

    @Override
    public String getCategory() { return mCategory; }

    @Override
    public List<SettingExtendedOld> getChildSettings() { return mChildSettings; }

    /*@Override
    public IRandomizer setSelectedOption(String optionKey) {
        int index = Str.tryParseInt(optionKey);
        if(index < 0) return null;
        IRandomizer newRand = super.setSelectedOption(optionKey);
        for(SettingExtendedOld s : mChildSettings) s.updateFocusIndex(index);
        return newRand;
    }*/
}
