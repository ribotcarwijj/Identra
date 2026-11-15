package dev.identra.app.x.core.commands.call;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import dev.identra.app.ActivityMain;
import dev.identra.app.BuildConfig;
import dev.identra.app.DebugUtil;
import dev.identra.app.R;
import dev.identra.app.XLegacyCore;
import dev.identra.app.XParam;
import dev.identra.app.XUtil;
import dev.identra.app.x.Str;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.ui.adapters.hooks.elements.XHookIO;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.ActionFlag;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.database.DatabaseHelpEx;
import dev.identra.app.x.core.database.sql.SQLDatabase;
import dev.identra.app.x.core.hook.AssignmentApi;
import dev.identra.app.x.core.hook.AssignmentPacket;
import dev.identra.app.x.core.hook.AssignmentUtils;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.identity.UserIdentityUtils;
import dev.identra.app.x.core.settings.data.SettingPacket;
import dev.identra.app.x.core.settings.data.SettingsApi;

public class ReportCommand extends CallCommandHandlerEx {
    public static final String COMMAND_NAME = "report";
    private static final String TAG = LibUtil.generateTag(ReportCommand.class);
    final static String cChannelName = "xlua";

    public ReportCommand() {
        name = COMMAND_NAME;
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        Bundle bundle = commandData.extras;

        SQLDatabase database = commandData.getDatabase();
        int uid = bundle.getInt("uid");
        String pkg = bundle.getString("packageName");
        String hookId = bundle.getString("hook");
        long time = bundle.getLong("time");//installed, used
                                                //restricted = (1 or 0)

        if(Str.isEmpty(hookId) || AssignmentUtils.isFilterHook(hookId)) {
            if(DebugUtil.isDebug())
                Log.w(TAG, "Waring found a Filter Hook Report, take this with cation! Hook Id=" + hookId);
        }

        String event = bundle.getString("event");

        int userId = UserIdentityUtils.getUserId(uid);
        AssignmentPacket packet = database.executeWithReadLock(() -> AssignmentApi.getAssignment(database, userId, pkg, hookId));
        if(packet == null || Str.isEmpty(packet.getHookId())) {
            packet = new AssignmentPacket();
            packet.hook = hookId;
        }

        packet.setUserIdentity(UserIdentity.from(userId, uid, pkg));
        packet.setActionPacket(ActionPacket.create(ActionFlag.PUSH, false));
        Bundle sub = bundle.getBundle("data");

        //The reason no good is because it is being picked up as a RULE like (android_id) is Considered a Rule not a Hook
        //We need some way of resolving or

        if(event != null && sub != null) {
            packet.restricted = Str.toBool(String.valueOf(sub.getInt("restricted", 0)));
            Log.d(TAG, "Report: " + packet.getHookId() + " Event=" + event + " Restricted=" + packet.restricted);

            if(event.equalsIgnoreCase("use")) {
                packet.used = time;
                packet.newValue = sub.getString("new");
                packet.oldValue = sub.getString("old");

                //Log.e(TAG, "Trying to Send Notification...");
                try {
                    XHook hookObj = XLegacyCore.getHook(packet.getHookId());
                    //if(hookObj == null)
                    //    hookObj = XLegacyCore.getHook(packet.hook);

                    //Log.w(TAG, "Trying to Send Notification: " + hookObj.getObjectId());
                    //Ugh so close, this flag wont flip ...
                    //We can just set a flag on the creation of the hook like "setting.name.notify"
                    //We can get from caller, then pass in report bundle

                    if(hookObj != null && Boolean.TRUE.equals(hookObj.notify)) {
                        Log.d(TAG, "Notifying Object=" + XHookIO.toJsonString(hookObj));
                        long ident = Binder.clearCallingIdentity();
                        try {
                            Context ctx = XUtil.createContextForUser(commandData.getContext(), userId);
                            PackageManager pm = ctx.getPackageManager();
                            Resources resources = pm.getResourcesForApplication(BuildConfig.APPLICATION_ID);

                            String name = hookObj.group.toLowerCase().replaceAll("[^a-z]", "_");
                            int resId = resources.getIdentifier("group_" + name, "string", BuildConfig.APPLICATION_ID);
                            String group = (resId == 0 ? hookObj.getObjectId() : resources.getString(resId));

                            // Build notification
                            Notification.Builder builder = new Notification.Builder(ctx);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                builder.setChannelId(cChannelName);
                            builder.setSmallIcon(android.R.drawable.ic_dialog_info);
                            builder.setContentTitle(resources.getString(R.string.msg_usage, group));
                            builder.setContentText(pm.getApplicationLabel(pm.getApplicationInfo(commandData.getCategory(), 0)));
                            if (BuildConfig.DEBUG)
                                builder.setSubText(hookObj.getObjectId());

                            builder.setPriority(Notification.PRIORITY_DEFAULT);
                            builder.setCategory(Notification.CATEGORY_STATUS);
                            builder.setVisibility(Notification.VISIBILITY_SECRET);

                            // Main
                            Intent main = ctx.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
                            if (main != null) {
                                int flags = (Build.VERSION.SDK_INT > Build.VERSION_CODES.R ? 0x04000000 : 0);
                                main.putExtra(ActivityMain.EXTRA_SEARCH_PACKAGE, commandData.getCategory());
                                @SuppressLint("WrongConstant") PendingIntent pi = PendingIntent.getActivity(ctx, uid, main, flags);
                                builder.setContentIntent(pi);
                            }

                            builder.setAutoCancel(true);
                            XUtil.notifyAsUser(ctx, "xlua_use_" + hookObj.group, uid, builder.build(), userId);
                        }finally {
                            Binder.restoreCallingIdentity(ident);
                        }
                    }

                }catch (Exception e) {
                    Log.e(TAG, "Failed Notification: " + e);
                }

                // Get group name




            }
            else if(event.equalsIgnoreCase("install"))
                packet.installed = time;
            if (sub.containsKey("exception"))
                packet.exception = sub.getString("exception");
        }

        A_CODE result = DatabaseHelpEx.execute_one_locked_name(database, packet, AssignmentPacket.TABLE_INFO);
        if(DebugUtil.isDebug())
            Log.d(TAG, "Result=" + result.name() + " Hook ID=" + hookId);

        return null;
    }
}
