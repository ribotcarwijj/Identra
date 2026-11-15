package dev.identra.app.x.hook.filter.kinds;

import android.util.Log;

import java.util.List;
import java.util.Map;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.interceptors.ShellIntercept;
import dev.identra.app.interceptors.shell.handlers.GetPropIntercept;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.TypeMap;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.hook.filter.FilterContainerElement;
import dev.identra.app.x.hook.filter.IFilterContainer;
import dev.identra.app.x.hook.filter.PropRules;
import dev.identra.app.x.hook.filter.SettingPair;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.core.LibUtil;

public class GetPropFilterContainer extends FilterContainerElement implements IFilterContainer {
    private static final String TAG = LibUtil.generateTag(GetPropFilterContainer.class);


    public static IFilterContainer create() {  return new GetPropFilterContainer(); }

    public static final String GROUP_NAME = "Intercept.Properties";
    public static final TypeMap DEFINITIONS =
            TypeMap.create()
            .add("java.lang.System", "getProperty")
            .add("android.os.SystemProperties", "get");

    public GetPropFilterContainer() {
        super(GROUP_NAME, DEFINITIONS);
        this.dependencies.add(ShellFilterContainer.GROUP_NAME);
    }

    @Override
    public int appendSettings(Map<String, String> settings) {
        if(settings == null)
            return 0;

        int count = super.appendSettings(settings);
        if(ListUtil.isValid(getRules()) || count > 0) {
            settings.put(GetPropIntercept.GETPROP_INTERCEPT_SETTING, String.valueOf(true));
            count++;
        }

        return count;
    }

    @Override
    public boolean hasSwallowedAsRule(XHook hook) {
        boolean isRule = super.hasSwallowedAsRule(hook);
        if(isRule) {
            if(!hasSettings(hook) || !hasParams(hook)) {
                factory.removeRule(hook);
            } else {
                List<String> args = hook.parameterTypes;
                List<String> settings = hook.settings;
                if(DebugUtil.isDebug())
                    Log.d(TAG, Str.fm("Parsing Intercept Property Rule [%s] Properties=[%s] Settings=[%s]",
                            hook.getObjectId(),
                            Str.joinList(args),
                            Str.joinList(settings)));

                //We just put to the settings KEY="prop:property_name", VALUE="setting.name"
                for(int i = 0; i < args.size(); i++) {
                    String item = Str.trimOriginal(args.get(i));
                    if(Str.isEmpty(item))
                        continue;

                    SettingPair pair = new SettingPair(item, i, settings, null);
                    if(item.endsWith("*") && item.length() > 1) {
                        // 0.5.0: "persist.sys.oneplus.*" -> every key with that prefix (see PropRules)
                        putSettingPair(PropRules.PREFIX + item.substring(0, item.length() - 1), pair.settingName);
                    } else {
                        putSettingPair(createPropertySetting(pair.name), pair.settingName);
                    }
                    if(DebugUtil.isDebug())
                        Log.d(TAG, Str.fm("Pushed Property, Setting Name=%s Mapped Setting Name=%s",
                                pair.name,
                                pair.settingName));
                }
            }
        }

        return isRule;
    }
}
