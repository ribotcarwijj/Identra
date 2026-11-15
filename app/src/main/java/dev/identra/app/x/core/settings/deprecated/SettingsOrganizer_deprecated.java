package dev.identra.app.x.core.settings.deprecated;

import java.util.List;

import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.core.settings.SettingHolder;
import dev.identra.app.x.core.settings.SettingsContainer;
import dev.identra.app.x.core.settings.data.SettingPacket;

//Should make a Certain interface / thing for these types of things ? where it requires a ID and VALUE is a GHOST Data Object awaiting for Bind or Taken

@SuppressWarnings("all")
public class SettingsOrganizer_deprecated {
    private static final String TAG = "Identra.SettingsOrganizer";

    public static SettingsOrganizer_deprecated create() { return new SettingsOrganizer_deprecated(); }

    private SettingsMapHolder_deprecated map = new SettingsMapHolder_deprecated();

    public List<SettingsContainer> getSettingContainers() { return map.getContainers(); }
    public List<SettingHolder> getAllSettings() { return map.getAllSettings(); }

    public void parseList_final(List<SettingPacket> settings) {
        if(ListUtil.isValid(settings)) {
            for(SettingPacket setting : settings)
                map.pushSetting(setting);

            map.finalizeTransaction();
        }
    }

    //public static boolean isSingleOrContainer(NameInformation nameInformation) {  return nameInformation != null && (!nameInformation.endsWithNumber || nameInformation.hasChildren()); }
}
