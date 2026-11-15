package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.found.FoundIndex;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.ActionFlag;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.database.sql.SQLDatabase;
import dev.identra.app.x.core.hook.AssignmentApi;
import dev.identra.app.x.core.hook.AssignmentPacket;
import dev.identra.app.x.core.hook.AssignmentsPacket;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.identity.UserIdentityUtils;
import dev.identra.app.x.core.settings.data.SettingPacket;
import dev.identra.app.x.core.settings.data.SettingsApi;

/**
 * 0.6.0 observe mode, system_server side. A hooked app process reports the Identra hook ids that
 * fired in it ("Seen"). Any app may call this, but only about itself: the caller uid is taken from
 * the binder (put into the extras by GlobalCommandBridge before dispatch, client values are
 * overwritten) and the package must belong to that uid. Ids are merged into the per-app setting
 * {@link FoundIndex#SETTING_SEEN}. When "Use identity for everything found" is on for the app
 * ({@link FoundIndex#SETTING_AUTO}), new identity hooks with a value available are assigned too and
 * recorded in {@link FoundIndex#SETTING_AUTO_ADDED} (they take effect at the app's next start).
 */
public class FoundReportCommand extends CallCommandHandlerEx {
    private static final String TAG = "Identra.FoundReport";
    public static final String COMMAND_NAME = "identraFoundReport";
    /** Extra GlobalCommandBridge sets from Binder.getCallingUid(); never trusted from the client. */
    public static final String EXTRA_CALLER_UID = "identra.callerUid";
    public static final String EXTRA_PKG = "packageName";
    public static final String EXTRA_HOOKS = "hooks";
    private static final int MAX_IDS = 800;

    public FoundReportCommand() {
        name = COMMAND_NAME;
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket packet) throws Throwable {
        Bundle b = packet.extras;
        Bundle res = new Bundle();
        if (b == null) return res;
        int uid = b.getInt(EXTRA_CALLER_UID, -1);
        String pkg = b.getString(EXTRA_PKG);
        ArrayList<String> hooks = b.getStringArrayList(EXTRA_HOOKS);
        if (uid < 10000 || pkg == null || hooks == null || hooks.isEmpty()) return res;
        Context context = packet.getContext();
        if (!uidOwns(context, uid, pkg)) {
            Log.w(TAG, "uid " + uid + " reported for " + pkg + ", ignored");
            return res;
        }
        FoundIndex index = FoundIndex.get();
        Set<String> incoming = new HashSet<>();
        for (String h : hooks) if (h != null && index.byId.containsKey(h)) incoming.add(h);
        if (incoming.isEmpty()) return res;

        SQLDatabase db = packet.getDatabase();
        int userId = UserIdentityUtils.getUserId(uid);
        UserIdentity id = UserIdentity.fromUid(uid, pkg);
        synchronized (FoundReportCommand.class) {
            SettingPacket cur = SettingsApi.getSetting(db, userId, pkg, FoundIndex.SETTING_SEEN);
            Set<String> seen = FoundIndex.parseIds(cur == null ? null : cur.value);
            Set<String> added = new HashSet<>(incoming);
            added.removeAll(seen);
            if (added.isEmpty()) return res;
            seen.addAll(added);
            if (seen.size() > MAX_IDS) return res;
            put(db, context, id, pkg, userId, FoundIndex.SETTING_SEEN, FoundIndex.joinIds(seen));
            Log.i(TAG, pkg + " seen +" + added.size() + " = " + seen.size());

            SettingPacket auto = SettingsApi.getSetting(db, userId, pkg, FoundIndex.SETTING_AUTO);
            if (auto != null && "true".equalsIgnoreCase(auto.value)) autoAssign(context, db, uid, userId, pkg, id, added);
        }
        res.putInt("added", incoming.size());
        return res;
    }

    private static void autoAssign(Context context, SQLDatabase db, int uid, int userId, String pkg, UserIdentity id, Set<String> added) {
        FoundIndex index = FoundIndex.get();
        List<String> assign = new ArrayList<>();
        for (String hookId : added) {
            FoundIndex.Entry e = index.byId.get(hookId);
            if (e == null || !FoundIndex.isIdentity(e.group)) continue;
            AssignmentPacket a = AssignmentApi.getAssignment(db, userId, pkg, hookId);
            if (a != null && hookId.equals(a.getHookId())) continue;            // already assigned
            if (!hasValue(db, userId, pkg, e)) continue;
            assign.add(hookId);
        }
        if (assign.isEmpty()) return;
        A_CODE c = AssignmentApi.assignHooks(context, db, AssignmentsPacket.create(uid, pkg, assign, false, false));
        if (!A_CODE.isSuccessful(c)) { Log.w(TAG, "auto assign " + pkg + " -> " + c); return; }
        SettingPacket cur = SettingsApi.getSetting(db, userId, pkg, FoundIndex.SETTING_AUTO_ADDED);
        Set<String> all = FoundIndex.parseIds(cur == null ? null : cur.value);
        all.addAll(assign);
        put(db, context, id, pkg, userId, FoundIndex.SETTING_AUTO_ADDED, FoundIndex.joinIds(all));
        Log.i(TAG, pkg + " auto assigned " + assign);
    }

    /** Needs no value, or one of its values is set for the app or globally. */
    private static boolean hasValue(SQLDatabase db, int userId, String pkg, FoundIndex.Entry e) {
        boolean needs = false;
        for (String st : e.settings) {
            String low = st.trim().toLowerCase(java.util.Locale.ROOT);
            if (low.isEmpty() || low.endsWith(".bool") || low.startsWith("intercept.")) continue;
            needs = true;
            SettingPacket own = SettingsApi.getSetting(db, userId, pkg, st.trim());
            if (own != null && own.value != null && !own.value.isEmpty()) return true;
            SettingPacket glob = SettingsApi.getSetting(db, userId, UserIdentity.GLOBAL_NAMESPACE, st.trim());
            if (glob != null && glob.value != null && !glob.value.isEmpty()) return true;
        }
        return !needs;
    }

    private static void put(SQLDatabase db, Context context, UserIdentity id, String pkg, int userId, String name, String value) {
        SettingPacket p = new SettingPacket(name, value, ActionPacket.create(ActionFlag.PUSH, false), id);
        SettingsApi.single_locked(db, p, context, pkg, userId);
    }

    private static boolean uidOwns(Context context, int uid, String pkg) {
        long ident = Binder.clearCallingIdentity();
        try {
            String[] pkgs = context.getPackageManager().getPackagesForUid(uid);
            if (pkgs != null) for (String p : pkgs) if (pkg.equals(p)) return true;
            return false;
        } catch (Throwable t) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /** Client side (hooked app process). Fire and forget; returns false on an old system_server. */
    public static boolean report(Context context, ArrayList<String> hookIds) {
        try {
            Bundle b = new Bundle();
            b.putString(EXTRA_PKG, context.getPackageName());
            b.putStringArrayList(EXTRA_HOOKS, hookIds);
            Bundle r = XProxyContent.luaCall(context, COMMAND_NAME, b);
            return r != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
