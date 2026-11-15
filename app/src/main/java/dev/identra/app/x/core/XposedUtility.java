package dev.identra.app.x.core;

import android.os.Process;
import android.util.Log;

import dev.identra.xposed.XposedBridge;
import dev.identra.app.x.runtime.RuntimeUtils;

public class XposedUtility {


    public static boolean isUnderXposed() { return isUnderXposed(true); }
    public static boolean isUnderXposed(boolean doStackCheck) {
        if(Process.myUid() == Process.SYSTEM_UID)
            return true;

        if(!doStackCheck)
            return false;

        String stack = RuntimeUtils.getStackTraceSafeString().toLowerCase();
        return stack.contains("dev.identra.xposed.xposedbridge") ||
                stack.contains("io.github.libxposed") ||
                stack.contains("lsphooker") ||
                stack.contains("beforehook") ||
                stack.contains("afterhook");
    }

    public static void logD_xposed(String tag, String msg) {
        Log.d(tag, msg);
        if(isUnderXposed())
            XposedBridge.log("[" + tag + "] " + msg);
    }

    public static void logE_xposed(String tag, String msg) {
        Log.e(tag, msg);
        if(isUnderXposed())
            XposedBridge.log("[" + tag + "] " + msg);
    }

    public static void logI_xposed(String tag, String msg) {
        Log.i(tag, msg);
        if(isUnderXposed())
            XposedBridge.log("[" + tag + "] " + msg);
    }

    public static void logW_xposed(String tag, String msg) {
        Log.w(tag, msg);
        if(isUnderXposed())
            XposedBridge.log("[" + tag + "] " + msg);
    }
}
