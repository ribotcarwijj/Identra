package dev.identra.app.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;

import java.util.Arrays;
import java.util.List;

import dev.identra.app.x.core.commands.call.AssignHooksCommand;
import dev.identra.app.x.core.hook.AssignmentsPacket;

import dev.identra.app.x.core.commands.call.GetSettingExCommand;
import dev.identra.app.x.core.commands.call.GetVersionExCommand;
import dev.identra.app.x.core.commands.call.PutSettingExCommand;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.ActionFlag;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.settings.data.SettingPacket;

/**
 * Headless self-test of the Identra command channel, run in the app's own uid
 * through the same API the UI uses (getVersion, putExSettingEx, getExSettingEx).
 * Not exported: only root/system can trigger it, e.g. from a root shell:
 *   am broadcast -n dev.identra.app/.debug.SelfTestReceiver
 * Assign hooks + one setting to an app (used to verify a hook end-to-end):
 *   am broadcast -n dev.identra.app/.debug.SelfTestReceiver --es mode assign
 *      --es pkg com.example --es hooks "Identra.Build.MODEL,Identra.Build.MODEL/Rule"
 *      --es setting device.model --es value IdentraTest [--ez remove true]
 *   Several settings at once: --es settings "device.model=SM-S911B;device.brand=samsung"
 * Probe what this process sees (Identra is in its own scope, so hooks assigned to
 * dev.identra.app apply here; force-stop the app after assigning, then probe):
 *   am broadcast -n dev.identra.app/.debug.SelfTestReceiver --es mode probe
 *      [--es keys "ro.product.model,..."]
 * 0.6.0 "Found in this app" for one app (scan + seen, no changes):
 *   am broadcast -n dev.identra.app/.debug.SelfTestReceiver --es mode found --es pkg com.example [--ez rescan true]
 * 0.6.1 hot reload of every process running Identra (system_server + apps), result as text:
 *   am broadcast -n dev.identra.app/.debug.SelfTestReceiver --es mode reload
 * The report comes back as the broadcast's result data (printed by `am`).
 */
public class SelfTestReceiver extends BroadcastReceiver {
    private static final String KEY = "identra_selftest";

    /** Keys the probe reads through SystemProperties.get and `getprop` by default. */
    static final String[] PROBE_KEYS = {
            "ro.product.model", "ro.product.brand", "ro.product.manufacturer", "ro.product.device",
            "ro.build.fingerprint", "persist.sys.oneplus.serialno", "persist.sys.exif.model"};

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pending = goAsync();
        if ("reload".equals(intent.getStringExtra("mode"))) {
            dev.identra.app.reload.HotReloader.init();
            // the framework hands its binder to the app shortly after start; give it a moment
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() ->
                dev.identra.app.reload.HotReloader.reloadAll(out -> {
                    String r = (out.noService ? "FAIL noService" : (out.systemReloaded() ? "PASS" : "DONE"))
                            + " system=" + out.systemOk + "/" + out.systemTargets + ";apps=" + out.appOk + "/" + out.appTargets
                            + ";msg=" + out.message + ";systemBuild=" + dev.identra.app.x.core.commands.call.GetBuildCommand.get(context);
                    pending.setResultCode(out.systemReloaded() ? 1 : 0);
                    pending.setResultData(r);
                    pending.finish();
                }), 2000);
            return;
        }
        // 0.7.3: what an app gets: assigned hook ids (filtered by --es match) and resolved settings (filtered by --es keys regex)
        //   am broadcast -n dev.identra.app/.debug.SelfTestReceiver --es mode dump --es pkg com.example [--es match "Build|uname"] [--es keys "kernel|boot"]
        if ("dump".equals(intent.getStringExtra("mode"))) {
            new Thread(() -> {
                String r;
                try {
                    String pkg = intent.getStringExtra("pkg");
                    int uid = context.getPackageManager().getApplicationInfo(pkg, 0).uid;
                    String match = intent.getStringExtra("match");
                    String keys = intent.getStringExtra("keys");
                    java.util.regex.Pattern hm = match == null ? null : java.util.regex.Pattern.compile(match);
                    java.util.regex.Pattern km = keys == null ? null : java.util.regex.Pattern.compile(keys);
                    StringBuilder sb = new StringBuilder("PASS ");
                    java.util.Collection<dev.identra.app.x.ui.adapters.hooks.elements.XHook> hooks =
                            dev.identra.app.x.core.commands.query.GetAssignedHooksExCommand.get(context, true, uid, pkg);
                    sb.append("hooks=").append(hooks == null ? 0 : hooks.size()).append(';');
                    if (hooks != null && hm != null) for (dev.identra.app.x.ui.adapters.hooks.elements.XHook h : hooks)
                        if (h != null && h.getObjectId() != null && hm.matcher(h.getObjectId()).find()) sb.append("H:").append(h.getObjectId()).append(';');
                    java.util.Map<String, String> local = dev.identra.app.x.core.commands.query.GetSettingsExCommand.getAsMap(
                            context, true, uid, pkg, dev.identra.app.x.core.commands.query.GetSettingsExCommand.FLAG_ONE);
                    java.util.Map<String, String> global = dev.identra.app.x.core.commands.query.GetSettingsExCommand.getAsMap(
                            context, true, UserIdentity.DEFAULT_USER, UserIdentity.GLOBAL_NAMESPACE, dev.identra.app.x.core.commands.query.GetSettingsExCommand.FLAG_ONE);
                    java.util.TreeMap<String, String> all = new java.util.TreeMap<>();
                    if (global != null) all.putAll(global);
                    if (local != null) for (java.util.Map.Entry<String, String> e : local.entrySet()) if (e.getValue() != null) all.put(e.getKey(), e.getValue());
                    sb.append("settings=").append(all.size()).append(';');
                    if (km != null) for (java.util.Map.Entry<String, String> e : all.entrySet())
                        if (km.matcher(e.getKey()).find()) sb.append("S:").append(e.getKey()).append('=').append(e.getValue()).append(';');
                    r = sb.toString();
                } catch (Throwable t) { r = "FAIL error=" + t; }
                pending.setResultCode(r.startsWith("PASS") ? 1 : 0);
                pending.setResultData(r);
                pending.finish();
            }).start();
            return;
        }
        if ("found".equals(intent.getStringExtra("mode"))) {
            new Thread(() -> {
                String r;
                try {
                    String pkg = intent.getStringExtra("pkg");
                    int uid = context.getPackageManager().getApplicationInfo(pkg, 0).uid;
                    dev.identra.app.found.FoundStore.Found f = dev.identra.app.found.FoundStore.load(
                            context, uid, pkg, intent.getBooleanExtra("rescan", false));
                    StringBuilder sb = new StringBuilder("PASS found=").append(f.ids.size()).append(";seen=").append(f.seenCount())
                            .append(";identityOn=").append(dev.identra.app.found.FoundStore.isIdentityOn(context, pkg, uid)).append(';');
                    for (java.util.Map.Entry<String, Boolean> e : f.ids.entrySet())
                        sb.append(e.getValue() ? "S:" : "L:").append(e.getKey()).append(';');
                    r = sb.toString();
                } catch (Throwable t) { r = "FAIL error=" + t; }
                pending.setResultCode(r.startsWith("PASS") ? 1 : 0);
                pending.setResultData(r);
                pending.finish();
            }).start();
            return;
        }
        if ("probe".equals(intent.getStringExtra("mode"))) {
            new Thread(() -> {
                String r;
                try { r = "PASS " + probe(intent.getStringExtra("keys")); } catch (Throwable t) { r = "FAIL error=" + t; }
                pending.setResultCode(r.startsWith("PASS") ? 1 : 0);
                pending.setResultData(r);
                pending.finish();
            }).start();
            return;
        }
        if ("assign".equals(intent.getStringExtra("mode"))) {
            new Thread(() -> {
                String r;
                try { r = assign(context, intent); } catch (Throwable t) { r = "FAIL error=" + t; }
                pending.setResultCode(r.startsWith("PASS") ? 1 : 0);
                pending.setResultData(r);
                pending.finish();
            }).start();
            return;
        }
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            boolean ok = true;
            try {
                int v = GetVersionExCommand.get(context);
                sb.append("getVersion=").append(v).append(';');
                ok &= v > 0;
                // 0.5.1: protocol this APK expects; system_server answering less runs older code
                sb.append("protocol=").append(dev.identra.IdentraNs.PROTOCOL).append(';');
                int build = dev.identra.app.x.core.commands.call.GetBuildCommand.get(context);
                sb.append("systemBuild=").append(build).append(";appBuild=").append(dev.identra.app.BuildConfig.VERSION_CODE).append(';');
                sb.append("systemServer=").append(v <= 0 ? "none" : build >= dev.identra.app.BuildConfig.VERSION_CODE ? "current" : "older").append(';');

                String stamp = "ok-" + System.currentTimeMillis();
                A_CODE put = PutSettingExCommand.putGen(context, KEY, stamp);
                sb.append("put=").append(put).append(';');
                ok &= put == A_CODE.SUCCESS;

                SettingPacket got = GetSettingExCommand.get(context, KEY, Process.myUid(), SettingPacket.GLOBAL_CATEGORY);
                String gv = got == null ? null : got.value;
                sb.append("get=").append(gv).append(';');
                ok &= stamp.equals(gv);

                SettingPacket del = new SettingPacket(KEY, null);
                del.setUserIdentity(UserIdentity.fromUid(Process.myUid(), UserIdentity.GLOBAL_NAMESPACE));
                del.setActionPacket(ActionPacket.create(ActionFlag.DELETE, false));
                A_CODE dc = PutSettingExCommand.call(context, del);
                sb.append("delete=").append(dc).append(';');

                SettingPacket after = GetSettingExCommand.get(context, KEY, Process.myUid(), SettingPacket.GLOBAL_CATEGORY);
                String av = after == null ? null : after.value;
                sb.append("afterDelete=").append(av).append(';');
                ok &= av == null || av.isEmpty();
            } catch (Throwable t) {
                ok = false;
                sb.append("error=").append(t);
            }
            pending.setResultCode(ok ? 1 : 0);
            pending.setResultData((ok ? "PASS " : "FAIL ") + sb);
            pending.finish();
        }).start();
    }

    private static String assign(Context context, Intent intent) throws Exception {
        String pkg = intent.getStringExtra("pkg");
        boolean remove = intent.getBooleanExtra("remove", false);
        int uid = context.getPackageManager().getApplicationInfo(pkg, 0).uid;
        List<String> hooks = Arrays.asList(intent.getStringExtra("hooks").split(","));
        StringBuilder sb = new StringBuilder();
        A_CODE a = AssignHooksCommand.call(context, AssignmentsPacket.create(uid, pkg, hooks, remove, true));
        sb.append("uid=").append(uid).append(";assign=").append(a).append(';');
        boolean ok = a == A_CODE.SUCCESS;
        java.util.LinkedHashMap<String, String> settings = new java.util.LinkedHashMap<>();
        if (intent.getStringExtra("setting") != null) settings.put(intent.getStringExtra("setting"), intent.getStringExtra("value"));
        String multi = intent.getStringExtra("settings");
        if (multi != null) for (String kv : multi.split(";")) {
            int eq = kv.indexOf('=');
            if (eq > 0) settings.put(kv.substring(0, eq).trim(), kv.substring(eq + 1));
        }
        for (java.util.Map.Entry<String, String> e : settings.entrySet()) {
            String name = e.getKey();
            String value = e.getValue();
            SettingPacket sp = new SettingPacket(name, remove ? null : value);
            sp.setUserIdentity(UserIdentity.fromUid(uid, pkg));
            sp.setActionPacket(ActionPacket.create(remove ? ActionFlag.DELETE : ActionFlag.PUSH, true));
            A_CODE s = PutSettingExCommand.call(context, sp);
            SettingPacket back = GetSettingExCommand.get(context, name, uid, pkg);
            sb.append(name).append('=').append(s).append(",readBack=").append(back == null ? null : back.value).append(';');
            ok &= s == A_CODE.SUCCESS;
        }
        return (ok ? "PASS " : "FAIL ") + sb;
    }

    /** What this process sees: Build fields, SystemProperties.get and `getprop` through Runtime.exec. */
    private static String probe(String keysArg) {
        String[] keys = keysArg == null || keysArg.isEmpty() ? PROBE_KEYS : keysArg.split(",");
        StringBuilder sb = new StringBuilder();
        sb.append("pid=").append(Process.myPid()).append(';');
        sb.append("Build.MODEL=").append(android.os.Build.MODEL).append(';');
        sb.append("Build.BRAND=").append(android.os.Build.BRAND).append(';');
        sb.append("Build.MANUFACTURER=").append(android.os.Build.MANUFACTURER).append(';');
        sb.append("Build.DEVICE=").append(android.os.Build.DEVICE).append(';');
        sb.append("Build.FINGERPRINT=").append(android.os.Build.FINGERPRINT).append(';');
        java.lang.reflect.Method get = null;
        try { get = Class.forName("android.os.SystemProperties").getMethod("get", String.class); }
        catch (Throwable t) { sb.append("SystemProperties=unavailable ").append(t).append(';'); }
        for (String k : keys) {
            k = k.trim();
            if (get != null) {
                try { sb.append("SP[").append(k).append("]=").append(get.invoke(null, k)).append(';'); }
                catch (Throwable t) { sb.append("SP[").append(k).append("]=error ").append(t).append(';'); }
            }
            sb.append("getprop[").append(k).append("]=").append(exec(new String[]{"getprop", k})).append(';');
        }
        return sb.toString();
    }

    private static String exec(String[] cmd) {
        java.lang.Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            StringBuilder out = new StringBuilder();
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) { if (out.length() > 0) out.append(' '); out.append(line); }
            }
            p.waitFor();
            return out.toString();
        } catch (Throwable t) {
            return "error " + t;
        } finally {
            if (p != null) p.destroy();
        }
    }
}
