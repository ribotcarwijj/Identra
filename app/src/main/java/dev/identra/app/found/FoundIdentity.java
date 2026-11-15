package dev.identra.app.found;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import dev.identra.app.ui.IdentraData;
import dev.identra.app.ui.profiles.Profile;
import dev.identra.app.ui.profiles.ProfileApplier;
import dev.identra.app.ui.profiles.ProfileKeys;
import dev.identra.app.ui.profiles.ProfileStore;
import dev.identra.app.x.core.commands.call.ForceStopAppCommand;
import dev.identra.app.x.core.commands.call.PutSettingExCommand;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.ActionFlag;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.settings.data.SettingPacket;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;

/**
 * 0.6.0 "Use identity for everything found": one switch that assigns every found identity hook
 * whose values the app's profile (or the global default profile) provides, and removes exactly
 * those again when it is turned off. While it is on, system_server adds hooks seen later on its own
 * (setting {@link FoundIndex#SETTING_AUTO}) and {@link #sync} adds new scan results.
 * All methods block; call them off the main thread.
 */
public final class FoundIdentity {
    private static final String TAG = "Identra.FoundId";

    private FoundIdentity() { }

    public static final class Result {
        public boolean ok = true;
        public boolean noProfile;
        public int assigned, removed, skippedNoValue, skippedNotIdentity;
        public String profileName;
        public String error;
    }

    /** The profile whose values the app uses: its own, else the global default. */
    public static Profile profileFor(Context context, AppXpPacket app) {
        ProfileStore store = ProfileStore.get(context);
        Profile p = store.profileFor(app.packageName, app.uid);
        return p != null ? p : store.global();
    }

    public static Result enable(Context context, AppXpPacket app) {
        Result res = new Result();
        Profile profile = profileFor(context, app);
        if (profile == null) { res.ok = false; res.noProfile = true; return res; }
        res.profileName = profile.name;
        FoundStore.Found found = FoundStore.load(context, app.uid, app.packageName, false);
        List<XHook> pick = pickAssignable(context, app, found.ids.keySet(), profile, res);
        Set<String> managed = new LinkedHashSet<>(FoundStore.managed(context, app.packageName, app.uid));
        Set<String> already = IdentraData.assignedIds(app);
        List<XHook> toAssign = new ArrayList<>();
        for (XHook h : pick) {
            String id = h.getObjectId();
            if (already.contains(id)) continue;          // assigned by hand before: not ours to remove later
            toAssign.add(h);
            managed.add(id);
        }
        if (!toAssign.isEmpty()) {
            A_CODE c = assignBlocking(context, app, toAssign);
            if (!A_CODE.isSuccessful(c)) { res.ok = false; res.error = String.valueOf(c); return res; }
        }
        res.assigned = toAssign.size();
        try { ProfileApplier.syncVendorHideFor(context, app, profile); } catch (Throwable ignored) { }
        putSetting(context, app, FoundIndex.SETTING_AUTO, "true");
        FoundStore.setState(context, app.packageName, app.uid, true, managed);
        ForceStopAppCommand.stop(context, app.uid, app.packageName);
        Log.i(TAG, "on " + app.packageName + " profile=" + profile.name + " assigned=" + res.assigned
                + " noValue=" + res.skippedNoValue + " notIdentity=" + res.skippedNotIdentity);
        return res;
    }

    public static Result disable(Context context, AppXpPacket app) {
        Result res = new Result();
        Set<String> ids = new HashSet<>(FoundStore.managed(context, app.packageName, app.uid));
        ids.addAll(FoundStore.autoAdded(context, app.uid, app.packageName));
        putSetting(context, app, FoundIndex.SETTING_AUTO, null);
        putSetting(context, app, FoundIndex.SETTING_AUTO_ADDED, null);
        if (!ids.isEmpty()) {
            final CountDownLatch latch = new CountDownLatch(1);
            final A_CODE[] code = {A_CODE.SUCCESS};
            boolean started = IdentraData.unassign(context, app, ids, c -> { code[0] = c; latch.countDown(); });
            if (started) {
                try { latch.await(60, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
                if (!A_CODE.isSuccessful(code[0])) { res.ok = false; res.error = String.valueOf(code[0]); }
            }
            res.removed = ids.size();
        }
        FoundStore.setState(context, app.packageName, app.uid, false, null);
        ForceStopAppCommand.stop(context, app.uid, app.packageName);
        return res;
    }

    /** Adds newly found identity hooks while the switch is on. Returns how many were added. */
    public static int sync(Context context, AppXpPacket app) {
        if (app == null || !FoundStore.isIdentityOn(context, app.packageName, app.uid)) return 0;
        Profile profile = profileFor(context, app);
        if (profile == null) return 0;
        FoundStore.Found found = FoundStore.load(context, app.uid, app.packageName, false);
        Result res = new Result();
        List<XHook> pick = pickAssignable(context, app, found.ids.keySet(), profile, res);
        Set<String> managed = new LinkedHashSet<>(FoundStore.managed(context, app.packageName, app.uid));
        managed.addAll(FoundStore.autoAdded(context, app.uid, app.packageName));
        Set<String> already = IdentraData.assignedIds(app);
        List<XHook> toAssign = new ArrayList<>();
        for (XHook h : pick) {
            String id = h.getObjectId();
            if (already.contains(id) || managed.contains(id)) continue;
            toAssign.add(h);
            managed.add(id);
        }
        if (toAssign.isEmpty()) return 0;
        A_CODE c = assignBlocking(context, app, toAssign);
        if (!A_CODE.isSuccessful(c)) return 0;
        FoundStore.setState(context, app.packageName, app.uid, true, managed);
        Log.i(TAG, "sync " + app.packageName + " added=" + toAssign.size());
        return toAssign.size();
    }

    /** Found ids -> hook objects that are identity hooks, available to the app, with values from the profile. */
    static List<XHook> pickAssignable(Context context, AppXpPacket app, Set<String> foundIds, Profile profile, Result res) {
        List<XHook> out = new ArrayList<>();
        IdentraData.Snapshot s = IdentraData.cached();
        if (s == null) {
            try { s = IdentraData.load(context); } catch (Throwable t) { return out; }
        }
        Set<String> bases = ProfileApplier.filledBases(context, profile);
        Map<String, XHook> available = new java.util.HashMap<>();
        for (XHook h : IdentraData.availableHooks(s, app)) available.put(h.getObjectId(), h);
        for (String id : foundIds) {
            XHook h = available.get(id);
            if (h == null) continue;
            if (!FoundIndex.isIdentity(h.group)) { res.skippedNotIdentity++; continue; }
            if (!hasValues(h, bases)) { res.skippedNoValue++; continue; }
            out.add(h);
        }
        return out;
    }

    /** A hook qualifies when it needs no value or the profile fills at least one of the values it reads. */
    static boolean hasValues(XHook h, Set<String> bases) {
        if (h.settings == null) return true;
        boolean any = false, needs = false;
        for (String st : h.settings) {
            if (st == null || st.trim().isEmpty()) continue;
            String low = st.trim().toLowerCase(java.util.Locale.ROOT);
            // flags (".bool") and intercept switches are switches, not identity values
            if (low.endsWith(".bool") || low.startsWith("intercept.")) continue;
            needs = true;
            if (bases.contains(ProfileKeys.baseOf(st))) { any = true; break; }
        }
        return !needs || any;
    }

    private static A_CODE assignBlocking(Context context, AppXpPacket app, List<XHook> hooks) {
        final CountDownLatch latch = new CountDownLatch(1);
        final A_CODE[] code = {A_CODE.SUCCESS};
        boolean started = IdentraData.assign(context, app, hooks, c -> { code[0] = c; latch.countDown(); });
        if (!started) return A_CODE.SUCCESS;
        try { latch.await(60, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
        return code[0];
    }

    private static void putSetting(Context context, AppXpPacket app, String name, String value) {
        try {
            SettingPacket p = new SettingPacket(name, value,
                    ActionPacket.create(value == null ? ActionFlag.DELETE : ActionFlag.PUSH, false),
                    UserIdentity.fromUid(app.uid, app.packageName));
            PutSettingExCommand.call(context, p);
        } catch (Throwable t) {
            Log.w(TAG, "setting " + name + ": " + t);
        }
    }
}
