package dev.identra.app.ui.tools;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.Gravity;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.identra.app.R;
import dev.identra.app.ui.IdentraData;
import dev.identra.app.ui.IdentraRows;
import dev.identra.app.ui.IdentraScreenActivity;
import dev.identra.app.ui.IdentraSheet;
import dev.identra.app.ui.profiles.AppDataVault;
import dev.identra.app.ui.profiles.Profile;
import dev.identra.app.ui.profiles.ProfileApplier;
import dev.identra.app.ui.profiles.ProfileGenerator;
import dev.identra.app.ui.profiles.ProfileStore;
import dev.identra.app.ui.profiles.ProfileUi;
import dev.identra.app.x.core.hook.AppXpPacket;

/**
 * 0.7.17 Instances: run an app as extra, isolated copies.
 * Each instance is a hidden CLONE profile (own uid, own data, own GSF/Ad ids). CloneKeeper in
 * system_server keeps everything but the chosen app from running there and hides the profile
 * from Settings, the launcher and other apps. All work goes through su.
 * Config lives in Settings.Global identra_clone_allow ("10=pkg|pkg;11=pkg").
 */
public class IdentraInstancesActivity extends IdentraScreenActivity {
    private static final String TAG = "Identra.UiInstances";
    private static final String KEY = "identra_clone_allow";
    // 0.7.31: Play Services set for an instance. Play Store is needed for Play licensing (PairIP
    // LicenseActivity blocked Play-licensed apps), Bluetooth for GMS (crashed on BLUETOOTH_CONNECT).
    static final String[] GMS_PKGS = { "com.google.android.gms", "com.google.android.gsf", "com.android.vending", "com.android.bluetooth" };
    private static final String GMS = String.join("|", GMS_PKGS);
    private static final String GMS_PERMS = "android.permission.BLUETOOTH_CONNECT android.permission.BLUETOOTH_SCAN android.permission.BLUETOOTH_ADVERTISE";
    static boolean isGmsPkg(String p) { for (String g : GMS_PKGS) if (g.equals(p)) return true; return false; }
    /** Shell that installs the Play Services set into a clone user and grants GMS its Bluetooth permissions. */
    static String gmsInstall(String user) {
        StringBuilder sb = new StringBuilder();
        for (String g : GMS_PKGS) sb.append("cmd package install-existing --user ").append(user).append(' ').append(g).append(" >/dev/null 2>&1\n");
        sb.append("for P in ").append(GMS_PERMS).append("; do pm grant --user ").append(user).append(" com.google.android.gms $P >/dev/null 2>&1; done\n");
        return sb.toString();
    }
    // packages a clone keeps installed besides the chosen app
    private static final String KEEP = "android|com\\.google\\.android\\.trichromelibrary.*|com\\.google\\.android\\.webview|com\\.android\\.webview"
            + "|com\\.google\\.android\\.gms|com\\.google\\.android\\.gsf|com\\.android\\.providers\\..*|com\\.google\\.android\\.providers\\..*"
            + "|com\\.android\\.permissioncontroller|com\\.google\\.android\\.permissioncontroller|com\\.android\\.networkstack.*|com\\.google\\.android\\.networkstack.*"
            + "|com\\.android\\.shell|com\\.android\\.externalstorage|com\\.android\\.keychain|com\\.android\\.certinstaller|.*inputmethod.*"
            + "|com\\.google\\.android\\.packageinstaller|com\\.android\\.packageinstaller|com\\.android\\.location\\.fused|com\\.android\\.se"
            + "|com\\.android\\.wifi.*|com\\.google\\.android\\.wifi.*|com\\.google\\.android\\.adservices.*|com\\.android\\.adservices.*"
            + "|com\\.google\\.android\\.ext\\.services|com\\.android\\.inputdevices|com\\.android\\.phone|dev\\.identra\\.app";

    private LinearLayout body;
    /** 0.7.20: work outlives the screen (a new user can recreate the activity); results go to whichever copy is alive. */
    private static java.lang.ref.WeakReference<IdentraInstancesActivity> live = new java.lang.ref.WeakReference<>(null);
    private static volatile String pending;   // "Creating instance…" etc, shown until the job ends
    private static final Handler MAIN_H = new Handler(Looper.getMainLooper());

    private static IdentraInstancesActivity alive() {
        IdentraInstancesActivity a = live.get();
        return a != null && !a.isFinishing() && !a.isDestroyed() ? a : null;
    }

    private static void onUi(java.util.function.Consumer<IdentraInstancesActivity> f) {
        MAIN_H.post(() -> { IdentraInstancesActivity a = alive(); if (a != null) try { f.accept(a); } catch (Throwable t) { Log.w(TAG, "ui: " + t); } });
    }
    private Map<Integer, List<String>> lastCfg = new LinkedHashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.identra_tool_instances));
        body = useScrollBody();
        live = new java.lang.ref.WeakReference<>(this);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        live = new java.lang.ref.WeakReference<>(this);
    }

    // ---------- model ----------

    public static Map<Integer, List<String>> parse(String raw) {
        Map<Integer, List<String>> m = new LinkedHashMap<>();
        if (raw == null || "null".equals(raw.trim())) return m;
        for (String part : raw.trim().split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            try {
                int u = Integer.parseInt(part.substring(0, eq).trim());
                List<String> pk = new ArrayList<>();
                for (String p : part.substring(eq + 1).split("[|,]")) if (!p.trim().isEmpty()) pk.add(p.trim());
                m.put(u, pk);
            } catch (NumberFormatException ignored) { }
        }
        return m;
    }

    static String format(Map<Integer, List<String>> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, List<String>> e : m.entrySet()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey()).append('=').append(String.join("|", e.getValue()));
        }
        return sb.toString();
    }

    private String label(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return String.valueOf(pm.getApplicationLabel(ai));
        } catch (Throwable t) { return pkg; }
    }

    private static String q(String s) { return "'" + s.replace("'", "") + "'"; }

    // ---------- UI ----------

    private void refresh() {
        setBusy(true);
        IdentraData.EXECUTOR.submit(() -> {
            AppDataVault.Result r = AppDataVault.run("settings get global " + KEY + "; echo '##'; pm list users", 30);
            onUi(a -> { a.setBusy(false); a.render(r); });
        });
    }

    private void render(AppDataVault.Result r) {
        body.removeAllViews();
        if (!r.ok && !r.output.contains("##")) {
            note(getString(R.string.identra_instances_noroot));
            return;
        }
        String[] parts = r.output.split("##", 2);
        Map<Integer, List<String>> cfg = parse(parts[0]);
        lastCfg = cfg;
        String users = parts.length > 1 ? parts[1] : "";
        ProfileStore store = ProfileStore.get(this);
        if (pending != null) {
            LinearLayout st = IdentraRows.card(body);
            IdentraRows.row(st, R.drawable.ic_identra_restart, pending, getString(R.string.identra_instances_wait), null);
            setBusy(true);
        }
        int n = 0;
        InstanceNames.keepOnly(this, cfg.keySet());
        for (Map.Entry<Integer, List<String>> e : cfg.entrySet()) {
            final int user = e.getKey();
            final List<String> apps = userApps(e.getValue());
            if (apps.isEmpty()) continue;   // stale "none" entries
            n++;
            boolean exists = users.contains("{" + user + ":");
            boolean gms = e.getValue().contains("com.google.android.gms");
            folder(user, InstanceNames.get(this, user), apps, gms, exists, cfg, store);
        }
        if (n == 0) {
            LinearLayout empty = IdentraRows.card(body);
            note(empty, getString(R.string.identra_instances_empty));
        }
        IdentraRows.section(body, getString(R.string.identra_instances_new_section));
        LinearLayout add = IdentraRows.card(body);
        IdentraRows.row(add, R.drawable.ic_identra_add, getString(R.string.identra_instances_new), getString(R.string.identra_instances_new_desc),
                v -> {
                    if (pending != null) { snack(pending); return; }
                    pickApps(getString(R.string.identra_instances_new), Collections.emptyList(), picked -> askGms(picked, cfg));
                });
        note(getString(R.string.identra_instances_note));
    }

    /** One instance drawn like a home-screen folder: name + ⋮ on top, app icons in a grid. */
    private void folder(int user, String name, List<String> apps, boolean gms, boolean exists,
                        Map<Integer, List<String>> cfg, ProfileStore store) {
        LinearLayout card = IdentraRows.card(body);
        int pad = IdentraRows.dp(this, 16);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(pad, pad, IdentraRows.dp(this, 4), 0);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setTextAppearance(R.style.TextAppearance_Identra_CardTitle);
        t.setText(name);
        TextView sub = new TextView(this);
        sub.setTextAppearance(R.style.TextAppearance_Identra_Caption);
        sub.setText(getString(R.string.identra_instances_meta, getResources().getQuantityString(R.plurals.identra_instances_apps_n, apps.size(), apps.size()),
                gms ? getString(R.string.identra_instances_gms_on) : getString(R.string.identra_instances_gms_off))
                + (exists ? "" : " · " + getString(R.string.identra_instances_missing)));
        titles.addView(t);
        titles.addView(sub);
        head.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView more = new ImageView(this);
        more.setImageResource(R.drawable.ic_identra_more);
        int mp = IdentraRows.dp(this, 12);
        more.setPadding(mp, mp, mp, mp);
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true);
        more.setBackgroundResource(tv.resourceId);
        more.setContentDescription(getString(R.string.identra_instances_options));
        more.setOnClickListener(v -> folderMenu(user, name, apps, gms, cfg));
        head.setOnClickListener(v -> folderMenu(user, name, apps, gms, cfg));
        head.addView(more, new LinearLayout.LayoutParams(IdentraRows.dp(this, 48), IdentraRows.dp(this, 48)));
        card.addView(head);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setPadding(IdentraRows.dp(this, 8), IdentraRows.dp(this, 8), IdentraRows.dp(this, 8), IdentraRows.dp(this, 12));
        for (String app : apps) grid.addView(appCell(user, app, apps, cfg, store), cellLp());
        View add = cell(getDrawable(R.drawable.ic_identra_add), getString(R.string.identra_instances_add_app), true);
        add.setOnClickListener(v -> pickApps(getString(R.string.identra_instances_apps), apps, sel -> editApps(user, apps, sel, cfg)));
        grid.addView(add, cellLp());
        // 0.7.26: fill the last row so every cell is a quarter wide, whatever the app count.
        for (int i = (apps.size() + 1) % 4; i != 0 && i < 4; i++) grid.addView(new android.widget.Space(this), cellLp());
        card.addView(grid);
    }

    private GridLayout.LayoutParams cellLp() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        return lp;
    }

    private View appCell(int user, String app, List<String> apps, Map<Integer, List<String>> cfg, ProfileStore store) {
        Drawable d;
        try { d = getPackageManager().getApplicationIcon(app); } catch (Throwable t) { d = getDrawable(R.drawable.ic_identra_apps); }
        View c = cell(d, label(app), false);
        Profile prof = store.profileFor(app, cloneUid(user, app));
        if (prof != null) {
            ImageView badge = c.findViewWithTag("badge");
            badge.setVisibility(View.VISIBLE);
        }
        c.setOnClickListener(v -> open(user, app));
        c.setOnLongClickListener(v -> { appSheet(user, app, apps, cfg); return true; });
        return c;
    }

    private View cell(Drawable icon, CharSequence label, boolean tint) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = IdentraRows.dp(this, 6);
        c.setPadding(p, p, p, p);
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        c.setBackgroundResource(tv.resourceId);
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(icon);
        int s = IdentraRows.dp(this, 52);
        if (tint) {
            iv.setPadding(IdentraRows.dp(this, 14), IdentraRows.dp(this, 14), IdentraRows.dp(this, 14), IdentraRows.dp(this, 14));
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bg.setColor(com.google.android.material.color.MaterialColors.getColor(c, com.google.android.material.R.attr.colorSecondaryContainer));
            iv.setBackground(bg);
            iv.setImageTintList(android.content.res.ColorStateList.valueOf(
                    com.google.android.material.color.MaterialColors.getColor(c, com.google.android.material.R.attr.colorOnSecondaryContainer)));
        }
        frame.addView(iv, new android.widget.FrameLayout.LayoutParams(s, s));
        ImageView badge = new ImageView(this);
        badge.setTag("badge");
        badge.setImageResource(R.drawable.ic_identra_shield);
        badge.setImageTintList(android.content.res.ColorStateList.valueOf(
                com.google.android.material.color.MaterialColors.getColor(c, androidx.appcompat.R.attr.colorPrimary)));
        badge.setVisibility(View.GONE);
        android.widget.FrameLayout.LayoutParams bl = new android.widget.FrameLayout.LayoutParams(IdentraRows.dp(this, 18), IdentraRows.dp(this, 18));
        bl.gravity = Gravity.END | Gravity.BOTTOM;
        bl.setMargins(0, 0, -IdentraRows.dp(this, 4), -IdentraRows.dp(this, 2));
        frame.addView(badge, bl);
        // 0.7.26: wrap the icon frame; a match_parent frame left the icon at the start and the badge at the far end.
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fl.gravity = Gravity.CENTER_HORIZONTAL;
        c.addView(frame, fl);
        TextView t = new TextView(this);
        t.setTextAppearance(R.style.TextAppearance_Identra_Caption);
        t.setText(label);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setMaxLines(1);
        t.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.topMargin = IdentraRows.dp(this, 6);
        c.addView(t, tl);
        return c;
    }

    private void folderMenu(int user, String name, List<String> apps, boolean gms, Map<Integer, List<String>> cfg) {
        IdentraSheet sheet = new IdentraSheet(this).title(name)
                .sub(getString(R.string.identra_instances_meta, getResources().getQuantityString(R.plurals.identra_instances_apps_n, apps.size(), apps.size()),
                        gms ? getString(R.string.identra_instances_gms_on) : getString(R.string.identra_instances_gms_off)));
        LinearLayout c = IdentraRows.card(sheet.content());
        IdentraRows.row(c, R.drawable.ic_identra_add, getString(R.string.identra_instances_apps), null,
                v -> { sheet.dismiss(); pickApps(getString(R.string.identra_instances_apps), apps, sel -> editApps(user, apps, sel, cfg)); });
        IdentraRows.row(c, R.drawable.ic_identra_edit, getString(R.string.identra_instances_rename), null, v -> { sheet.dismiss(); rename(user, name); });
        IdentraRows.row(c, R.drawable.ic_identra_layers,
                getString(gms ? R.string.identra_instances_gms_turn_off : R.string.identra_instances_gms_turn_on),
                getString(R.string.identra_instances_gms_sub), v -> { sheet.dismiss(); setGms(user, apps, !gms, cfg); });
        IdentraRows.row(c, R.drawable.ic_identra_close, getString(R.string.identra_instances_stop), getString(R.string.identra_instances_stop_sub), v -> {
            sheet.dismiss();
            StringBuilder sb = new StringBuilder();
            for (String a : apps) sb.append("am force-stop --user ").append(user).append(' ').append(q(a)).append('\n');
            shell(sb.toString(), false);
            snack(getString(R.string.identra_instances_stopped));
        });
        LinearLayout d = IdentraRows.card(sheet.content());
        IdentraRows.row(d, R.drawable.ic_identra_delete, getString(R.string.identra_instances_remove), getString(R.string.identra_instances_remove_sub),
                v -> { sheet.dismiss(); confirmRemove(user, cfg); });
        sheet.show();
    }

    private void rename(int user, String current) {
        IdentraSheet sheet = new IdentraSheet(this).title(getString(R.string.identra_instances_rename));
        final com.google.android.material.textfield.TextInputEditText et = sheet.input(getString(R.string.identra_prof_name), current, false);
        sheet.action(IdentraSheet.STYLE_FILLED, getString(R.string.identra_prof_save), v -> {
            String n = et.getText() == null ? "" : et.getText().toString().trim();
            if (n.isEmpty()) return;
            InstanceNames.set(this, user, n);
            refresh();
        }, true);
        sheet.show();
    }

    private void setGms(int user, List<String> apps, boolean on, Map<Integer, List<String>> cfg) {
        List<String> full = new ArrayList<>(apps);
        if (on) Collections.addAll(full, GMS_PKGS);
        Map<Integer, List<String>> m = new LinkedHashMap<>(cfg);
        m.put(user, full);
        String sh;
        if (on) sh = gmsInstall(String.valueOf(user)) + "cmd package install-existing --user " + user + " com.android.phone >/dev/null 2>&1\n";
        else { StringBuilder b = new StringBuilder(); for (String g : GMS_PKGS) b.append("am force-stop --user ").append(user).append(' ').append(g).append('\n'); sh = b.toString(); }
        shell(sh + "settings put global " + KEY + " " + q(format(m)) + "\n", true);
    }

    private void setAppIcon(View row, String pkg) {
        try {
            Drawable d = getPackageManager().getApplicationIcon(pkg);
            ImageView iv = row.findViewById(R.id.identraRowIcon);
            iv.setImageTintList(null);
            iv.setImageDrawable(d);
            int s = IdentraRows.dp(this, 36);
            iv.getLayoutParams().width = s;
            iv.getLayoutParams().height = s;
            iv.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) { }
    }

    /** uid of pkg inside a clone user (same appId as on the owner). */
    private int cloneUid(int user, String pkg) {
        try { return user * 100000 + getPackageManager().getApplicationInfo(pkg, 0).uid % 100000; }
        catch (Throwable t) { return -1; }
    }

    private static AppXpPacket packetFor(android.content.Context c, int user, String pkg) {
        AppXpPacket a = new AppXpPacket();
        a.packageName = pkg;
        try { a.uid = user * 100000 + c.getPackageManager().getApplicationInfo(pkg, 0).uid % 100000; } catch (Throwable t) { a.uid = -1; }
        a.label = labelOf(c, pkg) + " · " + user;
        a.enabled = true;
        return a;
    }

    private AppXpPacket packet(int user, String pkg) {
        AppXpPacket a = new AppXpPacket();
        a.packageName = pkg;
        a.uid = cloneUid(user, pkg);
        a.label = labelOf(ctx(), pkg) + " · " + user;
        a.enabled = true;
        return a;
    }

    // ---------- 0.7.22: one place for everything about an app copy ----------

    private void appSheet(int user, String app, List<String> apps, Map<Integer, List<String>> cfg) {
        ProfileStore store = ProfileStore.get(this);
        int uid = cloneUid(user, app);
        Profile cur = store.profileFor(app, uid);
        IdentraSheet sheet = new IdentraSheet(this).title(label(app) + " · " + InstanceNames.get(this, user)).sub(identityLine(cur));
        LinearLayout c = IdentraRows.card(sheet.content());
        IdentraRows.row(c, R.drawable.ic_identra_open, getString(R.string.identra_instances_open), null, v -> { sheet.dismiss(); open(user, app); });

        IdentraRows.section(sheet.content(), getString(R.string.identra_instances_identity_section));
        LinearLayout id = IdentraRows.card(sheet.content());
        IdentraRows.row(id, R.drawable.ic_identra_dice, getString(R.string.identra_instances_new_identity), getString(R.string.identra_instances_new_identity_sub),
                v -> { sheet.dismiss(); randomPreview(user, app); });
        if (cur != null && cur.temp)
            IdentraRows.row(id, R.drawable.ic_identra_download, getString(R.string.identra_instances_save_profile), getString(R.string.identra_instances_save_profile_sub),
                    v -> { sheet.dismiss(); saveAs(cur, null); });
        IdentraRows.row(id, R.drawable.ic_identra_device, getString(R.string.identra_instances_pick_identity), null, v -> {
            sheet.dismiss();
            ProfileUi.pickProfile(this, getString(R.string.identra_instances_pick_identity), null, p -> { if (p != null) use(user, app, p); });
        });
        if (cur != null)
            IdentraRows.row(id, R.drawable.ic_identra_restore, getString(R.string.identra_instances_real_identity), getString(R.string.identra_instances_real_identity_sub),
                    v -> { sheet.dismiss(); revert(user, Collections.singletonList(app)); });

        IdentraRows.section(sheet.content(), getString(R.string.identra_instances_more_section));
        LinearLayout more = IdentraRows.card(sheet.content());
        IdentraRows.row(more, R.drawable.ic_identra_shield, getString(R.string.identra_instances_protection), getString(R.string.identra_instances_protection_sub), v -> {
            sheet.dismiss();
            AppXpPacket pk = packetFor(this, user, app);
            if (pk.uid > 0) startActivity(dev.identra.app.ui.AppDetailActivity.intent(this, pk));
        });
        IdentraRows.row(more, R.drawable.ic_identra_home, getString(R.string.identra_instances_shortcut), getString(R.string.identra_instances_shortcut_sub),
                v -> { sheet.dismiss(); pinShortcut(user, app); });
        IdentraRows.row(more, R.drawable.ic_identra_database, getString(R.string.identra_instances_clear), getString(R.string.identra_instances_clear_sub),
                v -> { sheet.dismiss(); confirmClear(user, app); });
        IdentraRows.row(more, R.drawable.ic_identra_close, getString(R.string.identra_instances_force_stop), null, v -> {
            sheet.dismiss();
            shell("am force-stop --user " + user + " " + q(app), false);
            snack(getString(R.string.identra_instances_stopped));
        });
        if (apps.size() > 1)
            IdentraRows.row(more, R.drawable.ic_identra_delete, getString(R.string.identra_instances_remove_app), null, v -> {
                sheet.dismiss();
                List<String> rest = new ArrayList<>(apps);
                rest.remove(app);
                editApps(user, apps, rest, cfg);
            });
        sheet.show();
    }

    private void confirmClear(int user, String app) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.identra_instances_clear) + " · " + label(app))
                .setMessage(R.string.identra_instances_clear_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.identra_instances_clear, (d, w) -> {
                    shell("am force-stop --user " + user + " " + q(app) + "; pm clear --user " + user + " " + q(app), false);
                    snack(getString(R.string.identra_instances_cleared));
                }).show();
    }

    private void pinShortcut(int user, String app) {
        try {
            android.content.pm.ShortcutManager sm = getSystemService(android.content.pm.ShortcutManager.class);
            if (sm == null || !sm.isRequestPinShortcutSupported()) { snack(getString(R.string.identra_instances_shortcut_unsupported)); return; }
            Drawable d = getPackageManager().getApplicationIcon(app);
            int sz = IdentraRows.dp(this, 108);
            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(sz, sz, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas cv = new android.graphics.Canvas(bmp);
            int inset = sz / 6;
            d.setBounds(inset, inset, sz - inset, sz - inset);
            d.draw(cv);
            Drawable badge = getDrawable(R.drawable.ic_identra_layers);
            if (badge != null) {
                android.graphics.Paint pt = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                pt.setColor(0xFF1A73E8);
                int r = sz / 7;
                cv.drawCircle(sz - inset, sz - inset, r, pt);
                badge.setTint(0xFFFFFFFF);
                badge.setBounds(sz - inset - r * 2 / 3, sz - inset - r * 2 / 3, sz - inset + r * 2 / 3, sz - inset + r * 2 / 3);
                badge.draw(cv);
            }
            String name = label(app) + " " + InstanceNames.get(this, user).replace("Instance ", "#");
            android.content.pm.ShortcutInfo si = new android.content.pm.ShortcutInfo.Builder(this, "inst-" + user + "-" + app)
                    .setShortLabel(name)
                    .setLongLabel(label(app) + " · " + InstanceNames.get(this, user))
                    .setIcon(android.graphics.drawable.Icon.createWithAdaptiveBitmap(bmp))
                    .setIntent(InstanceLaunchActivity.intent(this, user, app))
                    .build();
            sm.requestPinShortcut(si, null);
        } catch (Throwable t) {
            Log.w(TAG, "shortcut: " + t);
            snack(getString(R.string.identra_instances_shortcut_unsupported));
        }
    }

    private String identityLine(Profile p) {
        if (p == null) return getString(R.string.identra_instances_identity_real);
        if (p.temp) return getString(R.string.identra_instances_identity_temp, p.deviceSummary());
        return getString(R.string.identra_instances_identity, p.name);
    }

    /** Make a random identity, show it, then Use (unsaved) or Save &amp; use. */
    private void randomPreview(int user, String app) {
        final android.content.Context ctx = getApplicationContext();
        setBusy(true);
        ProfileApplier.EXEC.submit(() -> {
            Profile p = Profile.create(label(app) + " · " + user);
            ProfileGenerator.randomize(ctx, p, null, null, false);
            p.temp = true;
            onUi(a -> { a.setBusy(false); a.showPreview(user, app, p); });
        });
    }

    private void showPreview(int user, String app, Profile p) {
        IdentraSheet sheet = new IdentraSheet(this).title(getString(R.string.identra_instances_new_identity)).sub(label(app));
        String[][] keys = {{"Device", null}, {"Android ID", "unique.android.id"}, {"GSF ID", "unique.gsf.id"},
                {"Advertising ID", "unique.google.advertising.id"}, {"IMEI", "cell.unique.imei"}, {"Serial", "unique.serial.no"},
                {"Wi-Fi MAC", "unique.network.mac.address"}, {"Bluetooth", "unique.bluetooth.address"}};
        for (String[] k : keys) {
            String v = k[1] == null ? p.deviceSummary() : p.get(k[1]);
            if (v != null && !v.isEmpty()) sheet.field(k[0], v);
        }
        sheet.action(IdentraSheet.STYLE_TONAL, getString(R.string.identra_instances_again), v -> randomPreview(user, app), true);
        sheet.action(IdentraSheet.STYLE_OUTLINED, getString(R.string.identra_instances_use), v -> use(user, app, p), true);
        sheet.action(IdentraSheet.STYLE_FILLED, getString(R.string.identra_instances_save_use), v -> saveAs(p, () -> use(user, app, p)), true);
        sheet.show();
    }

    /** Name it and keep it under Profiles. */
    private void saveAs(Profile p, Runnable then) {
        IdentraSheet sheet = new IdentraSheet(this).title(getString(R.string.identra_instances_save_profile));
        final com.google.android.material.textfield.TextInputEditText et = sheet.input(getString(R.string.identra_prof_name),
                ProfileStore.get(this).uniqueName(p.deviceSummary().isEmpty() ? p.name : p.deviceSummary()), false);
        sheet.action(IdentraSheet.STYLE_FILLED, getString(R.string.identra_prof_save), v -> {
            String n = et.getText() == null ? "" : et.getText().toString().trim();
            if (n.isEmpty()) return;
            p.name = ProfileStore.get(this).uniqueName(n);
            p.temp = false;
            ProfileStore.get(this).put(p);
            snack(getString(R.string.identra_instances_saved, p.name));
            if (then != null) then.run(); else refresh();
        }, true);
        sheet.show();
    }

    /** Apply a profile to the app copy; an unsaved one it replaces is deleted. */
    private void use(int user, String app, Profile p) {
        final android.content.Context ctx = getApplicationContext();
        pending = getString(R.string.identra_instances_identity_busy);
        refresh();
        ProfileApplier.EXEC.submit(() -> {
            try {
                ProfileStore store = ProfileStore.get(ctx);
                int uid = packetFor(ctx, user, app).uid;
                Profile old = store.profileFor(app, uid);
                if (store.byId(p.id) == null) store.put(p);
                applyNow(ctx, user, app, p);
                if (old != null && !old.id.equals(p.id)) store.dropIfOrphan(old);
            } catch (Throwable t) {
                Log.e(TAG, "use: " + t);
            } finally {
                pending = null;
                onUi(a -> { a.snack(a.getString(R.string.identra_instances_identity_done)); a.refresh(); });
            }
        });
    }

    /** New copies start with their own unsaved random identity. */
    private void newIdentity(int user, List<String> apps) {
        pending = getString(R.string.identra_instances_identity_busy);
        refresh();
        background(getApplicationContext(), user, apps, () -> {
            pending = null;
            onUi(a -> a.refresh());
        });
    }

    /** Back to the real device: drop the profile values and the hooks for the app's clone uid. */
    private void revert(int user, List<String> apps) {
        final android.content.Context ctx = getApplicationContext();
        pending = getString(R.string.identra_instances_reverting);
        refresh();
        ProfileApplier.EXEC.submit(() -> {
            try {
                dev.identra.app.ui.IdentraData.Snapshot snap = null;
                try { snap = dev.identra.app.ui.IdentraData.load(ctx); } catch (Throwable t) { Log.w(TAG, "snapshot: " + t); }
                for (String app : apps) {
                    AppXpPacket pkt = packetFor(ctx, user, app);
                    if (pkt.uid < 0) continue;
                    AppXpPacket full = snap != null ? dev.identra.app.ui.IdentraData.findApp(snap, app, pkt.uid) : null;
                    ProfileApplier.Result r = ProfileApplier.removeFromApp(ctx, full != null ? full : pkt);
                    boolean hooks = full != null && dev.identra.app.ui.IdentraData.unassignAll(ctx, full, code -> Log.i(TAG, "unhook " + app + "/u" + user + ": " + code));
                    Log.i(TAG, "revert " + app + "/u" + user + " deleted=" + r.deleted + " unhook=" + hooks);
                    AppDataVault.run("am force-stop --user " + user + " " + q(app), 15);
                }
            } catch (Throwable t) {
                Log.e(TAG, "revert: " + t);
            } finally {
                pending = null;
                onUi(a -> { a.snack(a.getString(R.string.identra_instances_reverted)); a.refresh(); });
            }
        });
    }

    private static String labelOf(android.content.Context c, String pkg) {
        try {
            PackageManager pm = c.getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (Throwable t) { return pkg; }
    }

    /** Scope Identra into pkg/user (LSPosed), write the profile's values for the clone uid and turn on its hooks. Off-main. */
    private static void applyNow(android.content.Context ctx, int user, String app, Profile p) {
        AppXpPacket pkt = packetFor(ctx, user, app);
        if (pkt.uid < 0) return;
        AppDataVault.Result sc = AppDataVault.run("/data/adb/lspd/cli scope add dev.identra.app " + q(app) + "/" + user + " 2>&1", 30);
        Log.i(TAG, "scope " + app + "/" + user + ": " + sc.output.trim());
        ProfileApplier.Result res = ProfileApplier.applyToApp(ctx, p, pkt, ProfileStore.DATA_SHARED, true, null);
        Log.i(TAG, "identity " + p.name + " -> " + app + "/u" + user + " ok=" + res.ok + " written=" + res.written + " hooks=" + res.hooks + " err=" + res.error);
        AppDataVault.run("am force-stop --user " + user + " " + q(app), 15);
    }

    private void note(String s) { note(body, s); }

    private void note(LinearLayout parent, String s) {
        TextView t = new TextView(this);
        t.setTextAppearance(R.style.TextAppearance_Identra_Caption);
        t.setText(s);
        int p = IdentraRows.dp(this, 12);
        t.setPadding(p, p, p, p);
        parent.addView(t);
    }

    /** Apps the user chose for an instance (config minus GMS/GSF and "none"). */
    public static List<String> userApps(List<String> pk) {
        List<String> out = new ArrayList<>();
        for (String p : pk) if (!isGmsPkg(p) && !"com.android.phone".equals(p) && !"none".equals(p)) out.add(p);
        return out;
    }

    interface PickedApps { void onPicked(List<String> pkgs); }

    /** Multi-select of every launchable app on the owner profile. */
    private void pickApps(String title, List<String> preselected, PickedApps cb) {
        PackageManager pm = getPackageManager();
        Intent li = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        Map<String, String> byPkg = new LinkedHashMap<>();
        for (ResolveInfo ri : pm.queryIntentActivities(li, 0)) {
            String p = ri.activityInfo.packageName;
            if (getPackageName().equals(p) || byPkg.containsKey(p)) continue;
            byPkg.put(p, String.valueOf(ri.loadLabel(pm)));
        }
        for (String p : preselected) if (!byPkg.containsKey(p)) byPkg.put(p, label(p));
        List<String> pkgs = new ArrayList<>(byPkg.keySet());
        Collections.sort(pkgs, (a, b) -> {
            boolean sa = preselected.contains(a), sb = preselected.contains(b);
            if (sa != sb) return sa ? -1 : 1;
            return byPkg.get(a).compareToIgnoreCase(byPkg.get(b));
        });
        CharSequence[] names = new CharSequence[pkgs.size()];
        boolean[] checked = new boolean[pkgs.size()];
        for (int i = 0; i < pkgs.size(); i++) { names[i] = byPkg.get(pkgs.get(i)); checked[i] = preselected.contains(pkgs.get(i)); }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMultiChoiceItems(names, checked, (d, i, on) -> checked[i] = on)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    List<String> sel = new ArrayList<>();
                    for (int i = 0; i < pkgs.size(); i++) if (checked[i]) sel.add(pkgs.get(i));
                    if (sel.isEmpty()) Toast.makeText(this, R.string.identra_instances_pick_one, Toast.LENGTH_SHORT).show();
                    else cb.onPicked(sel);
                }).show();
    }

    private void open(int user, String app) {
        setBusy(true);
        IdentraData.EXECUTOR.submit(() -> {
            AppDataVault.Result r = AppDataVault.run(InstanceLaunchActivity.script(user, app), 30);
            onUi(a -> {
                a.setBusy(false);
                if (!r.output.contains("OK")) a.snack(a.getString(R.string.identra_instances_launch_failed));
            });
        });
    }

    /** Add/remove apps in an existing instance: install-existing the new ones, drop the old ones, rewrite the allowlist. */
    private void editApps(int user, List<String> before, List<String> after, Map<Integer, List<String>> cfg) {
        List<String> full = new ArrayList<>(after);
        List<String> old = cfg.get(user);
        if (old != null && old.contains("com.google.android.gms")) Collections.addAll(full, GMS_PKGS);
        Map<Integer, List<String>> m = new LinkedHashMap<>(cfg);
        m.put(user, full);
        StringBuilder sb = new StringBuilder();
        for (String a : after) if (!before.contains(a)) sb.append("cmd package install-existing --user ").append(user).append(' ').append(q(a)).append(" >/dev/null 2>&1\n");
        for (String a : before) if (!after.contains(a)) sb.append("am force-stop --user ").append(user).append(' ').append(q(a))
                .append("; cmd package uninstall -k --user ").append(user).append(' ').append(q(a)).append(" >/dev/null 2>&1\n");
        sb.append("settings put global ").append(KEY).append(' ').append(q(format(m))).append('\n');
        List<String> added = new ArrayList<>();
        for (String a : after) if (!before.contains(a)) added.add(a);
        shell(sb.toString(), true);
        if (!added.isEmpty()) newIdentity(user, added);
    }

    private void confirmRemove(int user, Map<Integer, List<String>> cfg) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.identra_instances_remove)
                .setMessage(R.string.identra_instances_remove_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.identra_instances_remove, (d, w) -> {
                    Map<Integer, List<String>> m = new LinkedHashMap<>(cfg);
                    List<String> apps = userApps(cfg.getOrDefault(user, Collections.emptyList()));
                    m.remove(user);
                    final android.content.Context ctx = getApplicationContext();
                    pending = getString(R.string.identra_instances_removing);
                    refresh();
                    ProfileApplier.EXEC.submit(() -> {
                        try {
                            for (String app : apps) {
                                AppXpPacket pk = packetFor(ctx, user, app);
                                if (pk.uid > 0) ProfileApplier.removeFromApp(ctx, pk);
                                AppDataVault.run("/data/adb/lspd/cli scope rm dev.identra.app " + q(app) + "/" + user + " >/dev/null 2>&1", 20);
                            }
                            try {
                                android.content.pm.ShortcutManager sm = ctx.getSystemService(android.content.pm.ShortcutManager.class);
                                List<String> ids = new ArrayList<>();
                                for (String app : apps) ids.add("inst-" + user + "-" + app);
                                sm.disableShortcuts(ids, ctx.getString(R.string.identra_instances_shortcut_gone));
                            } catch (Throwable ignored) { }
                            AppDataVault.run("settings put global " + KEY + " " + q(format(m)) + "; pm remove-user " + user, 90);
                            InstanceNames.forget(ctx, user);
                        } catch (Throwable t) {
                            Log.e(TAG, "remove: " + t);
                        } finally {
                            pending = null;
                            onUi(a -> a.refresh());
                        }
                    });
                }).show();
    }

    private void askGms(List<String> pkgs, Map<Integer, List<String>> cfg) {
        new AlertDialog.Builder(this)
                .setTitle(label(pkgs.get(0)) + (pkgs.size() > 1 ? " +" + (pkgs.size() - 1) : ""))
                .setMessage(R.string.identra_instances_gms_q)
                .setNegativeButton(R.string.identra_instances_gms_no, (d, w) -> create(pkgs, false, cfg))
                .setPositiveButton(R.string.identra_instances_gms_yes, (d, w) -> create(pkgs, true, cfg))
                .show();
    }

    private void create(List<String> pkgs, boolean gms, Map<Integer, List<String>> cfg) {
        pending = getString(R.string.identra_instances_creating);
        render(new AppDataVault.Result(true, format(cfg) + "##"));
        final android.content.Context app = getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            long t0 = System.currentTimeMillis();
            String allow = String.join("|", pkgs) + (gms ? "|" + GMS : "");
            String mine = String.join("|", pkgs).replace(".", "\\.");
            StringBuilder inst = new StringBuilder();
            for (String p : pkgs) inst.append("cmd package install-existing --user $U ").append(q(p)).append(" >/dev/null 2>&1\n");
            if (gms) { inst.append(gmsInstall("$U")); mine = mine + "|" + GMS.replace(".", "\\."); }
            // 0.7.18: allowlist first (CloneKeeper blocks everything else from the very start),
            // trim with `cmd package` (binder, no app_process spin-up) in 6 parallel lanes, start the user last.
            String script =
                    "O=$(pm create-user --profileOf 0 --user-type android.os.usertype.profile.CLONE IdentraClone 2>&1)\n"
                  + "U=$(echo \"$O\" | grep -o 'user id [0-9]*' | grep -o '[0-9]*')\n"
                  + "[ -z \"$U\" ] && { echo \"ERR $O\"; exit 3; }\n"
                  + "OLD=$(settings get global " + KEY + "); [ \"$OLD\" = null ] && OLD=''\n"
                  + "NEW=\"$U=" + allow + "\"; [ -n \"$OLD\" ] && NEW=\"$OLD;$NEW\"\n"
                  + "settings put global " + KEY + " \"$NEW\"\n"
                  + inst
                  + "cmd package list packages --user $U | cut -d: -f2 | grep -vE '^(" + KEEP + "|" + mine + ")$' "
                  + "| xargs -n 1 -P 6 cmd package uninstall -k --user $U >/dev/null 2>&1\n"
                  + "am start-user $U >/dev/null 2>&1\n"
                  + "echo \"OK $U $(cmd package list packages --user $U | wc -l)\"\n";
            AppDataVault.Result r = AppDataVault.run(script, 240);
            Log.i(TAG, "create " + pkgs + " in " + (System.currentTimeMillis() - t0) + "ms: " + r.output);
            pending = null;
            Matcher m = Pattern.compile("OK (\\d+)").matcher(r.output);
            if (m.find()) {
                int u = Integer.parseInt(m.group(1));
                MAIN_H.post(() -> {
                    Toast.makeText(app, R.string.identra_instances_created, Toast.LENGTH_SHORT).show();
                    IdentraInstancesActivity a = alive();
                    if (a != null) a.newIdentity(u, pkgs);
                    else background(app, u, pkgs, null);
                });
                return;
            }
            onUi(a -> {
                a.setBusy(false);
                new AlertDialog.Builder(a).setTitle(R.string.identra_instances_failed)
                        .setMessage(r.output.trim()).setPositiveButton(android.R.string.ok, null).show();
                a.refresh();
            });
        });
    }

    private void shell(String script, boolean reload) {
        setBusy(true);
        IdentraData.EXECUTOR.submit(() -> {
            AppDataVault.Result r = AppDataVault.run(script, 60);
            Log.i(TAG, "shell: " + r.output);
            onUi(a -> { a.setBusy(false); if (reload) a.refresh(); });
        });
    }

    /** Identity for a fresh instance when the screen is gone. */
    private static void background(android.content.Context ctx, int user, List<String> apps, Runnable done) {
        ProfileApplier.EXEC.submit(() -> {
            try {
                ProfileStore store = ProfileStore.get(ctx);
                for (String app : apps) {
                    Profile p = Profile.create(labelOf(ctx, app) + " · " + user);
                    ProfileGenerator.randomize(ctx, p, null, null, false);
                    p.temp = true;
                    store.put(p);
                    applyNow(ctx, user, app, p);
                }
            } catch (Throwable t) { Log.e(TAG, "background identity: " + t); }
            finally { if (done != null) done.run(); }
        });
    }
}
