package dev.identra.app.hooks;

import dev.identra.IdentraNs;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import org.luaj.vm2.Varargs;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import dev.identra.xposed.XC_MethodHook;
import dev.identra.xposed.XposedBridge;
import dev.identra.app.XSecurity;

import dev.identra.app.api.hook.IdentraHook;


public class XReporter {
    private static final String TAG = "Identra.XError";
    private final Map<String, Map<String, Bundle>> queue = new HashMap<String, Map<String, Bundle>>();
    private Timer timer = null;

    public void reportFieldException(
            Context context,
            Exception ex,
            IdentraHook hook,
            Field field) {

        StringBuilder sb = new StringBuilder();
        XReporter.writeExceptionHeader(sb, ex, context);
        XReporter.writeField(sb, field);
        reportError(context, hook, sb, "after");
    }

    public void reportMemberException(
            Context context,
            Exception ex,
            IdentraHook hook,
            Member member,
            String function,
            XC_MethodHook.MethodHookParam param) {

        StringBuilder sb = new StringBuilder();
        XReporter.writeExceptionHeader(sb, ex, context);
        XReporter.writeMethod(sb, member, function, param);
        reportError(context, hook, sb, function);
    }

    public static void writeExceptionHeader(StringBuilder sb, Exception ex, Context context) {
        sb.append("Exception:\n");
        sb.append(Log.getStackTraceString(ex));
        sb.append("\n");

        sb.append("\nPackage:\n");
        sb.append(context.getPackageName());
        sb.append(':');
        sb.append(Integer.toString(context.getApplicationInfo().uid));
        sb.append("\n");
    }

    public static void writeMethod(StringBuilder sb, Member member, String function, XC_MethodHook.MethodHookParam param) {
        sb.append("\nMethod:\n");
        sb.append(function);
        sb.append(' ');
        sb.append(member.toString());
        sb.append("\n");

        sb.append("\nArguments:\n");
        if (param.args == null)
            sb.append("null\n");
        else
            for (int i = 0; i < param.args.length; i++) {
                sb.append(i);
                sb.append(": ");
                if (param.args[i] == null)
                    sb.append("null");
                else {
                    sb.append(param.args[i].toString());
                    sb.append(" (");
                    sb.append(param.args[i].getClass().getName());
                    sb.append(')');
                }
                sb.append("\n");
            }

        sb.append("\nReturn:\n");
        if (param.getResult() == null)
            sb.append("null");
        else {
            sb.append(param.getResult().toString());
            sb.append(" (");
            sb.append(param.getResult().getClass().getName());
            sb.append(')');
        }
        sb.append("\n");
        // Report use error
        //Bundle data = new Bundle();
        //data.putString("function", function);
        //data.putString("exception", sb.toString());
        //report(context, hook.getId(), function, "use", data);
    }

    public static void writeField(StringBuilder sb, Field field) {
        sb.append("\nField:\n");
        sb.append(field.toString());
        sb.append("\n");
    }

    public void reportError(Context context, IdentraHook hook, StringBuilder sb, String functionName) {
        Log.e(TAG, sb.toString());
        // Report use error
        Bundle data = new Bundle();
        data.putString("function", functionName);
        data.putString("exception", sb.toString());
        pushReport(context, hook.getObjectId(), functionName, "use", data);
    }

    public void reportUsage(IdentraHook hook, Varargs result, long startTime, String funName, Context context) {
        boolean restricted = result.arg1().checkboolean();
        if (restricted && hook.doUsage()) {
            Bundle data = new Bundle();
            data.putString("function", funName);
            data.putInt("restricted", restricted ? 1 : 0);
            data.putLong("duration", SystemClock.elapsedRealtime() - startTime);
            if (result.narg() > 1) {
                data.putString("old", result.isnil(2) ? null : result.checkjstring(2));
                data.putString("new", result.isnil(3) ? null : result.checkjstring(3));
            }

            pushReport(context, hook.getObjectId(), funName, "use", data);
        }
    }

    public void pushReport(final Context context, String hook, String function, String event, final Bundle data) {
        final String packageName = context.getPackageName();
        final int uid = context.getApplicationInfo().uid;

        Bundle args = new Bundle();
        args.putString("hook", hook);
        args.putString("packageName", packageName);
        args.putInt("uid", uid);
        args.putString("event", event);
        args.putLong("time", new Date().getTime());
        args.putBundle("data", data);

        //
        //hook
        //packageName
        //uid
        //event => (use .....)
        //time
        //data  =>  [function]
        //          [restricted]
        //          [duration]
        //              [old]
        //              [new]
        //
        //
        //
        //
        //

        synchronized (queue) {
            String key = (function == null ? "*" : function) + ":" + event;
            if (!queue.containsKey(key))
                queue.put(key, new HashMap<String, Bundle>());
            queue.get(key).put(hook, args);

            if (timer == null) {
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    public void run() {
                        List<Bundle> work = new ArrayList<>();
                        synchronized (queue) {
                            for (String key : queue.keySet())
                                for (String hook : queue.get(key).keySet())
                                    work.add(queue.get(key).get(hook));
                            queue.clear();
                            timer = null;
                        }

                        for (Bundle args : work)
                            try {
                                context.getContentResolver()
                                        .call(XSecurity.getURI(), IdentraNs.CMD_LUA, "report", args);
                            } catch (Throwable ex) {
                                Log.e(TAG, Log.getStackTraceString(ex));
                                XposedBridge.log(ex);
                            }
                    }
                }, 1000);
            }
        }
    }

    //Report should happen on a diffent level ?
    //Dont hold everything up for this ?
    //Do note we also already lock db when needed ASSUMING it works by holding the thread then we have no need for a Single Threaded system
    //Just have threading to keep track of all threads
    //@SuppressLint("MissingPermission")
    //public static Bundle report(Context context, final Bundle extras, XDataBase db) {

    //}
}
