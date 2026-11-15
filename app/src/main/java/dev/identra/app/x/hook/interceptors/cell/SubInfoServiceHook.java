package dev.identra.app.x.hook.interceptors.cell;

import android.telephony.SubscriptionInfo;
import android.util.Log;

import java.util.List;

import dev.identra.xposed.XC_MethodHook;
import dev.identra.app.DebugUtil;
import dev.identra.app.XParam;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.core.LibUtil;

public class SubInfoServiceHook {
    private static final String TAG = LibUtil.generateTag(SubInfoServiceHook.class);



    public static boolean handleGetActiveSubscriptionInfoList(XC_MethodHook.MethodHookParam param) {
        try {
            List<SubscriptionInfo> subscriptions = (List<SubscriptionInfo>) param.getResult();
            String callingPackage = Str.ensureIsNotNullOrDefault(Str.toStringOrNull(param.args[0]), Str.EMPTY);
            String callingFeatureId = Str.ensureIsNotNullOrDefault(Str.toStringOrNull(param.args[1]), Str.EMPTY);
            boolean isForAllProfiles = (boolean) param.args[2];

            if(DebugUtil.isDebug())
                Log.d(TAG, Str.fm("Sub Count=%s Calling package=%s Calling feature id=%s Is For All Profiles=%s",
                        ListUtil.size(subscriptions),
                        callingPackage,
                        callingFeatureId,
                        isForAllProfiles));

        }catch (Exception e) {
            Log.e(TAG, "Got a list of Active Subscription Infos");
            return false;
        }

        return false;
    }


    public static boolean handleGetActiveSubscriptionInfoList(XParam param) {
        try {
            List<SubscriptionInfo> subscriptions = param.tryGetResult(ListUtil.emptyList());
            String callingPackage = param.tryGetArgument(0, Str.EMPTY);
            String callingFeatureId = param.tryGetArgument(1, Str.EMPTY);
            boolean isForAllProfiles = param.tryGetArgument(2, false);

            if(DebugUtil.isDebug())
                Log.d(TAG, Str.fm("Sub Count=%s Calling package=%s Calling feature id=%s Is For All Profiles=%s",
                        ListUtil.size(subscriptions),
                        callingPackage,
                        callingFeatureId,
                        isForAllProfiles));

        }catch (Exception e) {
            Log.e(TAG, "Got a list of Active Subscription Infos");
            return false;
        }

        return false;
    }
}
