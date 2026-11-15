package dev.identra.app.random.randomizers;

import android.text.Editable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.settings.LuaSettingExtended;
import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.random.ZoneRandom;
import dev.identra.app.random.elements.DataNullElement;
import dev.identra.app.random.elements.DataStringElement;
//import dev.identra.app.x.random.ILinkParent;
import dev.identra.app.random.elements.ISpinnerElement;
import dev.identra.app.random.zone.ZoneGenerator;
import dev.identra.app.x.data.utils.random.RandomGenerator;

/*public class RandomZoneParent implements IRandomizerOld, ILinkParent {
    //{ "name": "network.provider.isp", "description": "Set this and ignore the rest of the Network Properties as this will set everything up", "defaultValue": "Cox" },
    //{ "name": "network.dns", "description": "DNS Address (1.1.1.1)", "defaultValue": "1.1.1.1" },
    //{ "name": "network.dns.list", "description": "List of DNS Addresses Separated with a Comma", "defaultValue": "1.1.1.1" },
    //{ "name": "network.domains", "description": "A String  containing the comma separated domains to search when resolving host names on this link or leave empty for none", "defaultValue": "google.com" },
    //{ "name": "network.gateway", "description": "Wlan0 Gateway", "defaultValue": "1.1.1.1" },

    //{ "name": "network.host.address", "description": "Local Network Host Address your IPV4", "defaultValue": "127.0.0.1" },
    //{ "name": "network.host.name", "description": "Host name of Network (google.com)", "defaultValue": "google.com" },

    //public static List<String> CHILD_SETTINGS = Arrays.asList(
    //        "network.dns",
    //        "network.dns.list",
    //        "network.domains",
    //        "network.gateway",
    //        "network.host.address",
    //        "network.host.name",
    //        "network.netmask",
    //        "network.routes");

    private final List<ISpinnerElement> providers = new ArrayList<>();
    public RandomZoneParent() {
        providers.add(DataNullElement.EMPTY_ELEMENT);
        for(String e : ZoneRandom.TIMEZONE_IDS)
            providers.add(DataStringElement.create(e));
    }

    @Override
    public boolean isSetting(String setting) { return setting.equalsIgnoreCase(getSettingName()); }

    @Override
    public String getSettingName() {  return "zone.parent.control.tz"; }

    @Override
    public String getName() {
        return "TimeZone Parent Control";
    }

    @Override
    public String getID() {
        return "%tz_parent%";
    }

    @Override
    public String generateString() { return providers.get(RandomGenerator.nextInt(1, providers.size())).getValue(); }

    @Override
    public int generateInteger() { return 0; }

    @Override
    public List<ISpinnerElement> getOptions() { return this.providers; }

    @NonNull
    @Override
    public String toString() { return getName(); }

    @Override
    public List<LuaSettingExtended> randomizeSettings(List<LuaSettingExtended> settings) {
        List<LuaSettingExtended> newList = new ArrayList<>();
        ZoneGenerator gen = new ZoneGenerator();
        for(LuaSettingExtended s : settings) {
            String name = s.getName();
            if(!name.startsWith("zone")) {
                newList.add(s);
                continue;
            }

            if(!s.isSameNameAsDisplayCache()) {
                Editable e = s.getInputTextBox() != null ? s.getInputTextBox().getText() : null;
                if(DebugUtil.isDebug())
                    Log.d("Identra.RandomZoneParent", "Setting Name=" + s.getName() + " V=" + s.getValue() + " M=" + s.getModifiedValue() + " T=" + (e == null ? "null" : e.toString()));

                newList.add(s);
                continue;
            }

            if(name.equalsIgnoreCase(getSettingName())) {
                s.setModifiedValue(gen.getTimezoneID(), true);
            } else {
                switch (name) {
                    case "zone.timezone.id":
                        s.setModifiedValue(gen.getTimezoneID(), true);
                        break;
                    case "zone.country.iso":
                        s.setModifiedValue(gen.getCountryISO(), true);
                        break;
                    case "zone.country":
                        s.setModifiedValue(gen.getCountry(), true);
                        break;
                    case "zone.language":
                        s.setModifiedValue(gen.getLanguage(), true);
                        break;
                    case "zone.language.iso":
                        s.setModifiedValue(gen.getLanguageISO(), true);
                        break;
                    case "zone.language.tag":
                        s.setModifiedValue(gen.getLanguageTag(), true);
                        break;
                    case "zone.region":
                        s.setModifiedValue(gen.getRegion(), true);
                        break;
                    case "zone.timezone":
                        s.setModifiedValue(gen.getTimezoneOffset(), true);
                        break;
                    case "zone.timezone.display.name":
                        s.setModifiedValue(gen.getTimezoneDisplayName(), true);
                        break;
                    default:
                        newList.add(s);
                        break;
                }
            }
        }

        return newList;
    }
}*/
