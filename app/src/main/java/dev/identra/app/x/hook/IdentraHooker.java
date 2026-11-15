package dev.identra.app.x.hook;

import android.util.Log;

import dev.identra.xposed.callbacks.XC_LoadPackage;
import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.x.core.LibUtil;

public class IdentraHooker {
    private static final String TAG = LibUtil.generateTag(IdentraHooker.class);


    public static void handleWildcardParams(final XC_LoadPackage.LoadPackageParam lpparamm, IdentraHook hook) {
        try {



        }catch (Exception e) {
            Log.e(TAG, "Error Handling Wild Card Param Hook! Error=" + e);
        }
    }
}
