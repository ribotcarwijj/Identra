package dev.identra.app.ui.profiles;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.call.ClearAppDataCommand;
import dev.identra.app.x.core.commands.call.ForceStopAppCommand;
import dev.identra.app.x.core.commands.call.PutSettingExCommand;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.ActionFlag;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.settings.data.SettingPacket;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.ui.IdentraData;

/**
 * 0.4.0: writes a profile into the module. Per app: every filled value as a per-app setting
 * (UserIdentity.fromUid(uid, pkg), ActionFlag.PUSH through putExSettingEx: the same command the
 * values editor uses); values the previous profile set but this one leaves empty are deleted so
 * nothing stale is left behind. Optionally turns on the hook groups that read those keys. Global:
 * the same writes into the global namespace. All methods block; run them on {@link #EXEC}.
 */
public final class ProfileApplier {
    private static final String TAG = "Identra.ProfileApply";
    public static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    private ProfileApplier() { }

    public interface Progress { void step(String text); }

    public static final class Result {
        public boolean ok = true;
        public int written, deleted, failed, hooks;
        public String error;
        public String dataNote;

        void fail(String e) { ok = false; error = e; }
    }

    // ---------------------------------------------------------------------------------

    public static Result applyToApp(Context context, Profile profile, AppXpPacket app, String dataMode,
                                    boolean enableHooks, Progress progress) {
        Result res = new Result();
        ProfileStore store = ProfileStore.get(context);
        String pkg = app.packageName;
        int uid = app.uid;
        int userId = uid / 100000;
        if (Str.isEmpty(pkg) || UserIdentity.GLOBAL_NAMESPACE.equalsIgnoreCase(pkg)) { res.fail("invalid app"); return res; }
        ProfileStore.Assignment prev = store.record(pkg, uid);
        Profile prevProfile = prev == null ? null : store.byId(prev.profileId);
        String mode = dataMode == null ? ProfileStore.DATA_SHARED : dataMode;

        // ---- app data first: if it fails nothing else changes ----
        String owner = prev == null ? null : prev.dataOwner;
        if (ProfileStore.DATA_FRESH.equals(mode)) {
            step(progress, "Stopping " + IdentraData.labelOf(app) + "…");
            ForceStopAppCommand.stop(context, uid, pkg);
            step(progress, "Clearing app data…");
            A_CODE c = ClearAppDataCommand.clear(context, pkg);
            if (!A_CODE.isSuccessful(c)) { res.fail("Clearing app data failed (" + c + ")"); return res; }
            res.dataNote = "data cleared";
            owner = null;
        } else if (ProfileStore.DATA_SEPARATE.equals(mode)) {
            // whose data is in the app right now: the recorded owner, else the profile it used, else its original data
            String current = owner != null ? owner : (prevProfile != null ? prevProfile.id : ProfileStore.ORIGINAL);
            if (!current.equals(profile.id)) {
                if (!AppDataVault.hasRoot(false)) { res.fail("Root (su) is not available"); return res; }
                step(progress, "Stopping " + IdentraData.labelOf(app) + "…");
                AppDataVault.forceStop(pkg, userId);
                ForceStopAppCommand.stop(context, uid, pkg);
                String saveAs = current;
                if (ProfileStore.ORIGINAL.equals(saveAs) && AppDataVault.has(pkg, userId, ProfileStore.ORIGINAL))
                    saveAs = ProfileStore.ORIGINAL + "-" + System.currentTimeMillis();   // never overwrite the first original
                step(progress, "Saving current app data…");
                AppDataVault.Result s = AppDataVault.save(pkg, userId, saveAs);
                if (!s.ok) { res.fail("Saving app data failed, nothing was changed.\n" + tail(s.output)); return res; }
                step(progress, "Switching app data…");
                AppDataVault.Result r = AppDataVault.restoreOrFresh(pkg, userId, profile.id);
                if (!r.ok) { res.fail("Restoring app data failed. The previous data is saved as \"" + saveAs + "\".\n" + tail(r.output)); return res; }
                res.dataNote = r.output.contains("OK_RESTORED") ? "data restored" : "fresh data";
            }
            owner = profile.id;
        }

        // ---- values ----
        step(progress, "Writing " + profile.filledCount() + " values…");
        UserIdentity id = UserIdentity.fromUid(uid, pkg);
        writeValues(context, id, profile, prevProfile, res);

        // ---- hooks ----
        if (enableHooks) {
            step(progress, "Turning on matching protections…");
            res.hooks = enableHooksFor(context, app, profile);
            res.hooks += syncVendorHide(context, app, profile);
        }

        ProfileStore.Assignment a = new ProfileStore.Assignment();
        a.pkg = pkg;
        a.uid = uid;
        a.profileId = profile.id;
        a.dataMode = mode;
        // whose data the directories hold now: kept across a "shared" assignment so a later switch saves it correctly
        a.dataOwner = ProfileStore.DATA_SEPARATE.equals(mode) ? owner
                : ProfileStore.DATA_FRESH.equals(mode) ? null
                : (prev != null ? prev.dataOwner : null);
        store.setAssignment(a);

        ForceStopAppCommand.stop(context, uid, pkg);
        if (res.failed > 0 && res.written == 0) res.fail("No value could be written (" + res.failed + " failed)");
        Log.i(TAG, "apply " + profile.name + " -> " + pkg + " written=" + res.written + " deleted=" + res.deleted + " failed=" + res.failed + " hooks=" + res.hooks);
        return res;
    }

    /** Removes the profile's per-app values from the app and forgets the assignment (data is left as it is). */
    public static Result removeFromApp(Context context, AppXpPacket app) {
        Result res = new Result();
        ProfileStore store = ProfileStore.get(context);
        Profile p = store.profileFor(app.packageName, app.uid);
        if (p != null) {
            UserIdentity id = UserIdentity.fromUid(app.uid, app.packageName);
            for (String k : ProfileKeys.names()) {
                if (!p.has(k)) continue;
                if (A_CODE.isSuccessful(put(context, id, k, null, true))) res.deleted++;
            }
        }
        store.clearAssignment(app.packageName, app.uid);
        store.dropIfOrphan(p);
        syncVendorHide(context, app, null);
        ForceStopAppCommand.stop(context, app.uid, app.packageName);
        return res;
    }

    public static Result applyGlobal(Context context, Profile profile) {
        Result res = new Result();
        ProfileStore store = ProfileStore.get(context);
        Profile prev = store.global();
        UserIdentity id = UserIdentity.fromUid(Process.myUid(), UserIdentity.GLOBAL_NAMESPACE);
        writeValues(context, id, profile, prev, res);
        store.setGlobal(profile.id);
        if (res.failed > 0 && res.written == 0) res.fail("No value could be written");
        return res;
    }

    public static Result clearGlobal(Context context) {
        Result res = new Result();
        ProfileStore store = ProfileStore.get(context);
        Profile prev = store.global();
        if (prev != null) {
            UserIdentity id = UserIdentity.fromUid(Process.myUid(), UserIdentity.GLOBAL_NAMESPACE);
            for (String k : ProfileKeys.names())
                if (prev.has(k) && A_CODE.isSuccessful(put(context, id, k, null, true))) res.deleted++;
        }
        store.setGlobal(null);
        return res;
    }

    /** Re-writes the values of every app (and the global default) using this profile. No data operations. */
    public static Result reapplyEverywhere(Context context, Profile profile, Profile before, Progress progress) {
        Result total = new Result();
        ProfileStore store = ProfileStore.get(context);
        for (ProfileStore.Assignment a : store.usersOf(profile.id)) {
            step(progress, "Updating " + a.pkg + "…");
            Result r = new Result();
            writeValues(context, UserIdentity.fromUid(a.uid, a.pkg), profile, before, r);
            total.written += r.written;
            total.deleted += r.deleted;
            total.failed += r.failed;
            ForceStopAppCommand.stop(context, a.uid, a.pkg);
            a.applied = System.currentTimeMillis();
        }
        if (profile.id.equals(store.globalId())) {
            step(progress, "Updating the global default…");
            Result r = new Result();
            writeValues(context, UserIdentity.fromUid(Process.myUid(), UserIdentity.GLOBAL_NAMESPACE), profile, before, r);
            total.written += r.written;
            total.failed += r.failed;
        }
        store.save();
        return total;
    }

    // ---------------------------------------------------------------------------------

    private static void writeValues(Context context, UserIdentity id, Profile profile, Profile previous, Result res) {
        for (String k : ProfileKeys.names()) {
            if (profile.has(k)) {
                A_CODE c = put(context, id, k, profile.get(k), false);
                if (A_CODE.isSuccessful(c)) res.written++;
                else { res.failed++; Log.w(TAG, "put " + k + " -> " + c); }
            } else if (previous != null && previous.has(k)) {
                if (A_CODE.isSuccessful(put(context, id, k, null, true))) res.deleted++;
            }
        }
    }

    private static A_CODE put(Context context, UserIdentity id, String key, String value, boolean delete) {
        try {
            SettingPacket p = new SettingPacket(key, value, ActionPacket.create(delete ? ActionFlag.DELETE : ActionFlag.PUSH, false), id);
            return PutSettingExCommand.call(context, p);
        } catch (Throwable t) {
            Log.e(TAG, "put " + key + " failed: " + t);
            return A_CODE.FAILED;
        }
    }

    private static void step(Progress p, String s) { if (p != null) p.step(s); }

    private static String tail(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > 400 ? "…" + s.substring(s.length() - 400) : s;
    }

    // ---- hooks ------------------------------------------------------------------------

    private static Map<String, Set<String>> legacyByCanonical;

    /** canonical setting -> legacy names hooks may still list (from assets/remap_settings.json). */
    static synchronized Map<String, Set<String>> legacyNames(Context context) {
        if (legacyByCanonical != null) return legacyByCanonical;
        Map<String, Set<String>> map = new HashMap<>();
        try (InputStream in = context.getAssets().open("remap_settings.json")) {
            byte[] buf = new byte[in.available() > 0 ? in.available() : 65536];
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            String text = new String(bo.toByteArray(), StandardCharsets.UTF_8).replaceAll("(?m)^\\s*//.*$", "");
            JSONArray arr = new JSONArray(text);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String to = o.optString("id");
                JSONArray olds = o.optJSONArray("oldIds");
                if (to.isEmpty() || olds == null) continue;
                Set<String> s = map.get(to);
                if (s == null) { s = new HashSet<>(); map.put(to, s); }
                for (int j = 0; j < olds.length(); j++) s.add(olds.optString(j));
            }
        } catch (Throwable t) {
            Log.w(TAG, "remap not readable: " + t);
        }
        legacyByCanonical = map;
        return map;
    }

    /** 0.6.0: setting bases (canonical + legacy names) the profile fills; used by "Found in this app". */
    public static Set<String> filledBases(Context context, Profile profile) {
        Set<String> bases = new HashSet<>();
        if (profile == null) return bases;
        Map<String, Set<String>> legacy = legacyNames(context);
        for (String k : ProfileKeys.names()) {
            if (!profile.has(k)) continue;
            bases.add(ProfileKeys.baseOf(k));
            Set<String> olds = legacy.get(k);
            if (olds != null) for (String o : olds) bases.add(ProfileKeys.baseOf(o));
        }
        return bases;
    }

    /** 0.6.0: assigns or removes the vendor hide group for this app to match the profile. Blocking. */
    public static int syncVendorHideFor(Context context, AppXpPacket app, Profile profile) {
        return syncVendorHide(context, app, profile);
    }

    /** Hook groups (available to this app) whose hooks read at least one filled key of the profile. */
    public static Set<String> groupsFor(Context context, IdentraData.Snapshot s, AppXpPacket app, Profile profile) {
        Set<String> bases = new HashSet<>();
        Map<String, Set<String>> legacy = legacyNames(context);
        for (String k : ProfileKeys.names()) {
            if (!profile.has(k)) continue;
            bases.add(ProfileKeys.baseOf(k));
            Set<String> olds = legacy.get(k);
            if (olds != null) for (String o : olds) bases.add(ProfileKeys.baseOf(o));
        }
        Set<String> groups = new LinkedHashSet<>();
        for (XHook h : IdentraData.availableHooks(s, app)) {
            if (!usable(h)) continue;
            for (String st : h.settings) if (bases.contains(ProfileKeys.baseOf(st))) { groups.add(h.group); break; }
        }
        return groups;
    }

    private static boolean usable(XHook h) {
        if (h == null || Str.isEmpty(h.group)) return false;
        String g = h.group.toLowerCase(Locale.ROOT);
        if (g.startsWith("intercept.") || g.startsWith("test.") || g.startsWith("profile.")) return false;
        return h.enabled == null || Boolean.TRUE.equals(h.enabled);
    }

    // ---- 0.5.0: hide the real vendor's own properties when the profile is another brand ----

    /** Hook group whose rules hide the real vendor's properties (assets/unique/devices/oneplus). */
    public static final String VENDOR_HIDE_GROUP = "Profile.Hide.Vendor";

    /** Brands that share one vendor property namespace (OnePlus / OPPO / realme all use oplus). */
    static String brandFamily(String brand) {
        if (brand == null) return "";
        String b = brand.trim().toLowerCase(Locale.ROOT);
        if (b.equals("oneplus") || b.equals("oppo") || b.equals("realme") || b.equals("oplus")) return "oplus";
        return b;
    }

    /** True when the profile claims another brand than the real phone and we have a hide rule set for the real one. */
    public static boolean needsVendorHide(Profile profile) {
        if (profile == null) return false;
        String spoof = profile.get(ProfileKeys.BRAND);
        if (spoof == null || spoof.trim().isEmpty()) return false;
        String real = brandFamily(android.os.Build.BRAND);
        return "oplus".equals(real) && !real.equals(brandFamily(spoof));
    }

    /** Assigns or removes the vendor hide group for this app to match the profile (null = no profile). */
    private static int syncVendorHide(Context context, AppXpPacket app, Profile profile) {
        try {
            IdentraData.Snapshot s = IdentraData.cached();
            if (s == null) s = IdentraData.load(context);
            AppXpPacket live = IdentraData.findApp(s, app.packageName, app.uid);
            if (live == null) live = app;
            boolean hide = needsVendorHide(profile);
            final CountDownLatch latch = new CountDownLatch(1);
            boolean started;
            if (hide) {
                List<XHook> pick = new ArrayList<>();
                for (XHook h : IdentraData.availableHooks(s, live))
                    if (h.group != null && h.group.equalsIgnoreCase(VENDOR_HIDE_GROUP)) pick.add(h);
                if (pick.isEmpty()) return 0;
                started = IdentraData.assign(context, live, pick, c -> latch.countDown());
            } else {
                List<String> ids = IdentraData.assignedIdsInGroups(s, live, java.util.Collections.singletonList(VENDOR_HIDE_GROUP));
                if (ids.isEmpty()) return 0;
                started = IdentraData.unassign(context, live, ids, c -> latch.countDown());
            }
            if (!started) return 0;
            try { latch.await(30, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
            return hide ? 1 : 0;
        } catch (Throwable t) {
            Log.w(TAG, "vendor hide sync failed: " + t);
            return 0;
        }
    }

    private static int enableHooksFor(Context context, AppXpPacket app, Profile profile) {
        IdentraData.Snapshot s = IdentraData.cached();
        if (s == null) {
            try { s = IdentraData.load(context); } catch (Throwable t) { return 0; }
        }
        AppXpPacket live = IdentraData.findApp(s, app.packageName, app.uid);
        if (live == null) live = app;
        Set<String> groups = groupsFor(context, s, live, profile);
        if (groups.isEmpty()) return 0;
        List<XHook> pick = new ArrayList<>();
        Set<String> covered = new HashSet<>();
        for (XHook h : IdentraData.defaultHooks(s, live))
            if (groups.contains(h.group)) { pick.add(h); covered.add(h.group); }
        for (XHook h : IdentraData.availableHooks(s, live))
            if (usable(h) && groups.contains(h.group) && !covered.contains(h.group)) pick.add(h);
        if (pick.isEmpty()) return 0;
        final CountDownLatch latch = new CountDownLatch(1);
        final A_CODE[] code = {A_CODE.SUCCESS};
        boolean started = IdentraData.assign(context, live, pick, c -> { code[0] = c; latch.countDown(); });
        if (!started) return 0;
        try { latch.await(60, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
        return A_CODE.isSuccessful(code[0]) ? groups.size() : 0;
    }
}
