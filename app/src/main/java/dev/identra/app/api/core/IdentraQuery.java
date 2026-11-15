package dev.identra.app.api.core;

import android.content.Context;
import android.database.Cursor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.identra.app.api.app.IdentraApp;
import dev.identra.app.api.app.IdentraAppConversions;
import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.api.hook.IdentraHookConversions;
import dev.identra.app.api.xstandard.UserIdentityPacket;
import dev.identra.app.api.core.query.GetAppsCommand;
//import dev.identra.app.api.core.query.GetAssignedHooksCommand;
import dev.identra.app.api.core.query.GetHooksCommand;
import dev.identra.app.api.core.query.GetSettingsCommand;
import dev.identra.app.random.GlobalRandoms;
import dev.identra.app.random.IRandomizerOld;
import dev.identra.app.utilities.CursorUtil;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;

public class IdentraQuery {
    //Clean this class as well as security class
    public static Collection<IdentraApp> getApps(Context context, boolean marshall) { return IdentraAppConversions.fromCursor(GetAppsCommand.invoke(context, marshall), marshall, true); }
    public static Collection<XHook> getAllHooks(Context context, boolean marshall) {
        return IdentraHookConversions.fromCursor(GetHooksCommand.invoke(context, marshall, true), marshall, true);
    }


    public static Collection<XHook> getHooks(Context context, boolean marshall) { return IdentraHookConversions.fromCursor(GetHooksCommand.invoke(context, marshall), marshall, true); }

    public static Map<String, String> getGlobalSettings(Context context, int uid) { return getSettings(context, uid, UserIdentityPacket.GLOBAL_NAMESPACE, false); }
    public static Map<String, String> getGlobalSettings(Context context, int uid, String packageOrName) { return getSettings(context, uid, packageOrName, false); }
    public static Map<String, String> getSettings(Context context, int uid, String packageOrName, boolean randomizeRandoms) {
        final Map<String, String> settings = new HashMap<>();
        Cursor c = GetSettingsCommand.invoke(context, packageOrName, uid);
        try {
            while (c != null && c.moveToNext())
                settings.put(c.getString(0), c.getString(1));
        }finally {
            //if(c != null) c.close();
            CursorUtil.closeCursor(c);
        }

        if(randomizeRandoms && !settings.isEmpty()) {
            List<IRandomizerOld> randomizers = GlobalRandoms.getRandomizers();
            for (Map.Entry<String, String> p : settings.entrySet()) {
                if(p.getValue() != null) {
                    if(p.getValue().equalsIgnoreCase("%random%")) {
                        for(IRandomizerOld r : randomizers) {
                            if(r.isSetting(p.getKey().toLowerCase())) {
                                String randomValue = r.generateString();
                                settings.put(p.getKey(), randomValue);
                            }
                        }
                    }
                }
            }
        }

        return settings;
    }

   /* public static Collection<IdentraHook> getAssignments(Context context, String packageName, int uid, boolean marshall) {
        return IdentraHookConversions.fromCursor(
                !marshall ?
                GetAssignedHooksCommand.invoke(context, packageName, uid) :
                GetAssignedHooksCommand.invokeEx(context, packageName, uid),
                marshall, true);
    }*/
}
