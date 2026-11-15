package dev.identra.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import dev.identra.xposed.XposedBridge;
import dev.identra.app.api.xstandard.UserIdentityPacket;
import dev.identra.app.api.core.IdentraCall;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.call.GetSettingExCommand;

public class ReceiverPackage extends BroadcastReceiver {
    private static final String TAG = LibUtil.generateTag(ReceiverPackage.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String packageName = intent.getData().getSchemeSpecificPart();
            int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
            boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            Log.i(TAG, "Received " + intent + " uid=" + uid);

            int userid = XUtil.getUserId(uid);
            Context ctx = XUtil.createContextForUser(context, userid);


            if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()) && !packageName.startsWith(BuildConfig.APPLICATION_ID)) {
                // 0.6.0 "Found in this app": ask the Identra app to scan the new / updated APK
                long ident = android.os.Binder.clearCallingIdentity();
                try {
                    Intent scan = new Intent();
                    scan.setClassName(BuildConfig.APPLICATION_ID, "dev.identra.app.found.FoundScanReceiver");
                    scan.putExtra("pkg", packageName);
                    scan.putExtra("uid", uid);
                    scan.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    ctx.sendBroadcast(scan);
                } catch (Throwable t) {
                    Log.w(TAG, "found scan broadcast failed: " + t);
                } finally {
                    android.os.Binder.restoreCallingIdentity(ident);
                }
            }

            if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
                if (!replacing && !packageName.startsWith(BuildConfig.APPLICATION_ID)) {
                    // Initialize app

                    //Bundle args = new Bundle();
                    //args.putString("packageName", packageName);
                    //args.putInt("uid", uid);
                    //context.getContentResolver()
                    //        .call(XSecurity.getURI(), "xlua", "clearApp", args);
                    IdentraCall.clearApp(context, uid, packageName);

                    //Ensure this bullshit works (userid then use Global namespace ???????? )
                    //if (IdentraCall.getSettingBoolean(context, userid, UserIdentityPacket.GLOBAL_NAMESPACE, "restrict_new_apps"))
                        //IdentraCall.initApp(context, packageName, uid);
                    //    IdentraCall.initApp(context, uid, packageName);
                        //context.getContentResolver()
                        //        .call(XSecurity.getURI(), "xlua", "initApp", args);


                    // Notify new app
                    if (GetSettingExCommand.notifyOnNewApps(context, uid)) {
                        PackageManager pm = ctx.getPackageManager();
                        Resources resources = pm.getResourcesForApplication(BuildConfig.APPLICATION_ID);

                        Notification.Builder builder = new Notification.Builder(ctx);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            builder.setChannelId(XLegacyCore.cChannelName);
                        builder.setSmallIcon(android.R.drawable.ic_dialog_alert);
                        builder.setContentTitle(resources.getString(R.string.msg_review_settings));
                        builder.setContentText(pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)));

                        builder.setPriority(Notification.PRIORITY_HIGH);
                        builder.setCategory(Notification.CATEGORY_STATUS);
                        builder.setVisibility(Notification.VISIBILITY_SECRET);

                        // Main
                        int flags = (Build.VERSION.SDK_INT > Build.VERSION_CODES.R ? 0x04000000 : 0);
                        Intent main = ctx.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
                        main.putExtra(ActivityMain.EXTRA_SEARCH_PACKAGE, packageName);
                        @SuppressLint("WrongConstant") PendingIntent pi = PendingIntent.getActivity(ctx, uid, main, flags);
                        builder.setContentIntent(pi);

                        builder.setAutoCancel(true);

                        XUtil.notifyAsUser(ctx, "xlua_new_app", uid, builder.build(), userid);
                    }
                }
            } else if (Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(intent.getAction())) {
                if (BuildConfig.APPLICATION_ID.equals(packageName)) {
                    //Bundle args = new Bundle();
                    //args.putInt("user", userid);
                    //context.getContentResolver()
                    //        .call(XSecurity.getURI(), "xlua", "clearData", args);

                    //WTF IS THIS GLOBAL ARGUMENT ????????
                    //Clear Identra data ?
                    //IdentraCall.clearData(context, userid);
                    //For now we comment this out
                } else {
                    //Bundle args = new Bundle();
                    //args.putString("packageName", packageName);
                    //args.putInt("uid", uid);
                    //args.putBoolean("settings", true);
                    //context.getContentResolver()
                    //        .call(XSecurity.getURI(), "xlua", "clearApp", args);

                    IdentraCall.clearApp(context, uid, packageName, true);
                    XUtil.cancelAsUser(ctx, "xlua_new_app", uid, userid);
                }
            }
        } catch (Throwable ex) {
            Log.e(TAG, Log.getStackTraceString(ex));
            XposedBridge.log(ex);
        }
    }
}
