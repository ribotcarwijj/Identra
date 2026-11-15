package dev.identra.app.x.hook.inlined;

import android.util.Log;

import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.x.data.TypeMap;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.core.LibUtil;

import dev.identra.xposed.XC_MethodHook;
import dev.identra.xposed.XposedBridge;
import dev.identra.app.DebugUtil;
import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.TypeMap;
import dev.identra.app.x.runtime.RuntimeUtils;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.hook.PackageHookContext;


public class UpTimeHooks {
    private static final String TAG = LibUtil.generateTag(UpTimeHooks.class);
    private static final List<String> IDS = Arrays.asList("Identra.SystemClock.elapsedRealtime", "Identra.SystemClock.uptimeMillis");
    private static final TypeMap TYPE_MAP = TypeMap.create().add("android.os.SystemClock", "uptimeMillis");

    public static boolean attach(XHook hook, Member member) {
        if(hook == null || member == null)
            return false;
        try {
            if(!TYPE_MAP.hasDefinition(hook))
                return false;

            String id = hook.getObjectId();
            if(Str.isEmpty(id) || !IDS.contains(id))
                return false;
            if(DebugUtil.isDebug())
                Log.d(TAG, "Deploying [" + id + "] as a Fast Inlined Java Hook!");
            XposedBridge.hookMethod(member, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param)  {
                    Object res = param.getResult();
                    if(res instanceof Long) {
                        long val = (long)res;
                        long newVal = val + ThreadLocalRandom.current().nextLong(10L, 1001L);
                        param.setResult(newVal);
                    }
                }
            });
            return true;
        }catch (Exception e) {
            Log.e(TAG, "Failed to Attach System UpTime Fast Inlined Hooks! Hook=" + hook.getObjectId() + " Error=" + e);
            return false;
        }
    }
}
