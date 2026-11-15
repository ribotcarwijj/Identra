package dev.identra.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.identra.app.R;
import dev.identra.app.x.core.commands.call.PutSettingExCommand;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.core.hook.AssignmentPacket;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.ui.core.UserClientAppContext;

/** Per-app detail: one big switch, category cards, and the Advanced tools. */
public class AppDetailActivity extends AppCompatActivity {
    private static final String TAG = "Identra.UiDetail";
    private static final String EXTRA_PKG = "identra.pkg";
    private static final String EXTRA_UID = "identra.uid";

    public static Intent intent(Context context, AppXpPacket app) {
        Intent i = new Intent(context, AppDetailActivity.class);
        i.putExtra(EXTRA_PKG, app.packageName);
        i.putExtra(EXTRA_UID, app.uid);
        return i;
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private String pkg;
    private int uid;
    private IdentraData.Snapshot snapshot;
    private AppXpPacket app;
    private boolean groupsExpanded = false;

    private ImageView icon;
    private TextView name, status, pkgText;
    private LinearLayout locationBox;
    private MaterialToolbar toolbar;
    private MaterialSwitch mainSwitch;
    private LinearLayout categories, advanced;
    private View progress;

    // 0.6.0 Found in this app
    private LinearLayout foundBox;
    private dev.identra.app.found.FoundStore.Found found;
    private boolean foundLoading = false, foundBusy = false;
    private final java.util.Set<String> foundExpanded = new java.util.HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IdentraTheme.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identra_app_detail);

        toolbar = findViewById(R.id.identraDetailToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        pkgText = findViewById(R.id.identraDetailPkg);
        locationBox = findViewById(R.id.identraDetailLocation);
        // show the app name in the toolbar once the header has collapsed
        ((com.google.android.material.appbar.AppBarLayout) findViewById(R.id.identraDetailAppBar))
                .addOnOffsetChangedListener((bar, offset) -> {
                    boolean collapsed = Math.abs(offset) >= bar.getTotalScrollRange() - IdentraRows.dp(this, 8);
                    toolbar.setTitle(collapsed && app != null ? IdentraData.labelOf(app) : null);
                });
        findViewById(R.id.identraDetailSwitchRow).setOnClickListener(v -> mainSwitch.toggle());
        findViewById(R.id.identraDetailProfileChip).setOnClickListener(v -> {
            if (app != null) dev.identra.app.ui.profiles.ProfileUi.chooseForApp(this, app, () -> { reload(); renderLocation(); });
        });

        icon = findViewById(R.id.identraDetailIcon);
        name = findViewById(R.id.identraDetailName);
        status = findViewById(R.id.identraDetailStatus);
        mainSwitch = findViewById(R.id.identraDetailSwitch);
        categories = findViewById(R.id.identraDetailCategories);
        advanced = findViewById(R.id.identraDetailAdvanced);
        progress = findViewById(R.id.identraDetailProgress);
        foundBox = findViewById(R.id.identraDetailFound);

        pkg = getIntent().getStringExtra(EXTRA_PKG);
        uid = getIntent().getIntExtra(EXTRA_UID, -1);

        snapshot = IdentraData.cached();
        app = IdentraData.findApp(snapshot, pkg, uid);
        if (app != null) render();
        else reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderLocation();
    }

    /** This app's spoofed location: its own override, or the global one it inherits. */
    private void renderLocation() {
        if (locationBox == null || pkg == null) return;
        final Context ctx = getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            final IdentraLocation.Value own = IdentraLocation.readForApp(ctx, uid, pkg);
            final IdentraLocation.Value global = own == null ? IdentraLocation.readGlobal(ctx) : null;
            main.post(() -> {
                if (isFinishing()) return;
                locationBox.removeAllViews();
                String sub;
                if (own != null) {
                    String n = IdentraPlaces.nameFor(this, pkg, uid, own.lat, own.lon);
                    sub = getString(R.string.identra_detail_loc_own, (n != null ? n + " · " : "") + IdentraPlaces.shortCoords(own.lat, own.lon));
                } else if (global != null && global.set) {
                    String n = IdentraPlaces.nameFor(this, null, 0, global.lat, global.lon);
                    sub = getString(R.string.identra_detail_loc_global, n != null ? n : IdentraPlaces.shortCoords(global.lat, global.lon));
                } else {
                    sub = getString(R.string.identra_loc_not_set_desc);
                }
                IdentraRows.row(locationBox, R.drawable.ic_identra_location, getString(R.string.identra_detail_loc_title), sub,
                        v -> startActivity(HomeActivity.locationFor(this, pkg, uid)));
            });
        });
    }

    private void reload() {
        progress.setVisibility(View.VISIBLE);
        final Context ctx = getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            IdentraData.Snapshot s = null;
            try { s = IdentraData.load(ctx); } catch (Throwable t) { Log.e(TAG, "Load failed: " + t); }
            final IdentraData.Snapshot fs = s;
            main.post(() -> {
                if (isFinishing()) return;
                progress.setVisibility(View.GONE);
                if (fs != null && fs.stale) { reload(); return; }
                snapshot = IdentraData.cached();
                AppXpPacket found = IdentraData.findApp(snapshot, pkg, uid);
                if (found == null) {
                    Toast.makeText(this, R.string.identra_apps_loading_failed, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                app = found;
                render();
            });
        });
    }

    private void render() {
        if (app == null) return;
        int iconSize = IdentraRows.dp(this, 80);
        if (app.icon <= 0) icon.setImageResource(android.R.drawable.sym_def_app_icon);
        else UserClientAppContext.attachIcon(this, iconSize, icon, app.packageName, app.icon);
        name.setText(app.label == null || app.label.isEmpty() ? app.packageName : app.label);
        pkgText.setText(app.packageName);
        dev.identra.app.ui.profiles.Profile prof = dev.identra.app.ui.profiles.ProfileStore.get(this).profileFor(app.packageName, app.uid);
        ((com.google.android.material.chip.Chip) findViewById(R.id.identraDetailProfileChip))
                .setText(prof == null ? getString(R.string.identra_detail_profile_none) : getString(R.string.identra_detail_profile_named, prof.name));
        status.setText(IdentraData.isProtected(app)
                ? IdentraAppsAdapter.statusText(this, snapshot, app)
                : getString(R.string.identra_detail_off));

        mainSwitch.setOnCheckedChangeListener(null);
        mainSwitch.setChecked(IdentraData.isProtected(app));
        mainSwitch.setOnCheckedChangeListener((b, on) -> {
            if (on) {
                List<XHook> hooks = IdentraData.defaultHooks(snapshot, app);
                if (hooks.isEmpty()) {
                    snack(getString(R.string.identra_no_protections));
                    renderLater();
                    return;
                }
                IdentraData.assign(this, app, hooks, this::onResult);
                addScopeCard((LinearLayout) findViewById(R.id.identraDetailScope), true);
            } else {
                IdentraData.unassignAll(this, app, this::onResult);
            }
            renderLater();
        });

        renderFound();

        // ---- categories ----
        List<XHook> available = IdentraData.availableHooks(snapshot, app);
        Map<String, List<XHook>> byGroup = IdentraData.groupsOf(available);
        Map<String, List<String>> groupsByCat = new LinkedHashMap<>();
        for (String g : byGroup.keySet()) {
            String key = ProtectionCategories.categoryOf(g);
            List<String> l = groupsByCat.get(key);
            if (l == null) { l = new ArrayList<>(); groupsByCat.put(key, l); }
            l.add(g);
        }
        Set<String> assigned = IdentraData.assignedIds(app);

        categories.removeAllViews();
        addScopeCard((LinearLayout) findViewById(R.id.identraDetailScope), false);
        for (ProtectionCategories.Category c : ProtectionCategories.ALL) {
            List<String> groups = groupsByCat.get(c.key);
            if (groups == null || groups.isEmpty()) continue;
            addCategoryCard(categories, c, groups, byGroup, assigned);
        }
        if (categories.getChildCount() == 0) {
            TextView tv = new TextView(this);
            tv.setText(R.string.msg_no_groups);
            tv.setTextAppearance(R.style.TextAppearance_Identra_RowSub);
            tv.setPadding(IdentraRows.dp(this, 4), 0, IdentraRows.dp(this, 4), 0);
            categories.addView(tv);
        }

        // ---- advanced ----
        advanced.removeAllViews();
        List<String> otherGroups = groupsByCat.get(ProtectionCategories.OTHER);
        if (otherGroups != null && !otherGroups.isEmpty())
            addCategoryCard(advanced, ProtectionCategories.OTHER_CATEGORY, otherGroups, byGroup, assigned);

        LinearLayout tools = IdentraRows.card(advanced);
        IdentraRows.row(tools, R.drawable.ic_identra_tune, getString(R.string.identra_adv_app_settings),
                getString(R.string.identra_adv_app_values_desc), v -> startActivity(dev.identra.app.ui.tools.IdentraValuesActivity.forApp(this, app)));
        MaterialSwitch fs = IdentraRows.switchRow(tools, R.drawable.ic_identra_restart, getString(R.string.identra_adv_force_stop),
                getString(R.string.identra_adv_force_stop_desc), app.forceStop, (b, on) -> {
                    app.forceStop = on;
                    final Context ctx = getApplicationContext();
                    final int fUid = app.uid;
                    final String fPkg = app.packageName;
                    IdentraData.EXECUTOR.submit(() -> {
                        A_CODE code = PutSettingExCommand.putForceStop(ctx, fUid, fPkg, on);
                        main.post(() -> { if (!A_CODE.isSuccessful(code)) snack(getString(R.string.identra_change_failed, String.valueOf(code))); });
                    });
                });
        fs.setEnabled(!app.persistent);
        IdentraRows.row(tools, R.drawable.ic_identra_logs, getString(R.string.identra_adv_logs), getString(R.string.identra_adv_logs_desc),
                v -> startActivity(dev.identra.app.ui.tools.IdentraLogActivity.intent(this, app)));
        IdentraRows.row(tools, R.drawable.ic_identra_open, getString(R.string.identra_adv_launch), app.packageName, v -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (launch != null) startActivity(launch);
            else Toast.makeText(this, R.string.error_no_activity, Toast.LENGTH_SHORT).show();
        });

        // every individual group, like the classic expander
        LinearLayout groupsCard = IdentraRows.card(advanced);
        View groupsRow = IdentraRows.row(groupsCard, R.drawable.ic_identra_list, getString(R.string.identra_adv_groups),
                getString(R.string.identra_adv_groups_desc), v -> {
                    groupsExpanded = !groupsExpanded;
                    render();
                });
        groupsRow.findViewById(R.id.identraRowChevron).setRotation(groupsExpanded ? 90f : 0f);
        if (groupsExpanded) {
            for (Map.Entry<String, List<XHook>> e : byGroup.entrySet()) {
                final String group = e.getKey();
                final List<XHook> hooks = e.getValue();
                int on = 0;
                for (XHook h : hooks) if (assigned.contains(h.getObjectId())) on++;
                String sub = getString(ProtectionCategories.byKey(ProtectionCategories.categoryOf(group)).title);
                if (on > 0 && on < hooks.size()) sub = sub + " · " + on + "/" + hooks.size();
                IdentraRows.switchRow(groupsCard, 0, IdentraData.groupTitle(this, group), sub, on > 0, (b, checked) -> {
                    if (checked) IdentraData.assign(this, app, hooks, this::onResult);
                    else IdentraData.unassign(this, app, IdentraData.idsOf(hooks), this::onResult);
                    renderLater();
                });
            }
        }
    }

    private void addCategoryCard(LinearLayout parent, ProtectionCategories.Category c, final List<String> groups,
                                 Map<String, List<XHook>> byGroup, Set<String> assigned) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_identra_category, parent, false);
        ((ImageView) card.findViewById(R.id.identraCatIcon)).setImageResource(c.icon);
        ((TextView) card.findViewById(R.id.identraCatTitle)).setText(c.title);

        final List<XHook> hooks = new ArrayList<>();
        int groupsOn = 0;
        for (String g : groups) {
            List<XHook> gh = byGroup.get(g);
            if (gh == null) continue;
            hooks.addAll(gh);
            for (XHook h : gh) if (assigned.contains(h.getObjectId())) { groupsOn++; break; }
        }
        TextView desc = card.findViewById(R.id.identraCatDesc);
        if (groupsOn > 0 && groupsOn < groups.size())
            desc.setText(getString(R.string.identra_partial, groupsOn, groups.size()));
        else
            desc.setText(c.desc);

        final MaterialSwitch sw = card.findViewById(R.id.identraCatSwitch);
        sw.setChecked(groupsOn > 0);
        final String key = c.key;
        sw.setOnCheckedChangeListener((b, on) -> {
            if (on) {
                IdentraData.assign(this, app, hooks, this::onResult);
            } else {
                // remove everything of this category, including groups not currently available
                List<String> ids = new ArrayList<>();
                synchronized (app.assignments) {
                    for (AssignmentPacket a : app.assignments) {
                        String g = IdentraData.groupOfAssignment(snapshot, a);
                        String id = a == null ? null : a.getHookId();
                        if (g != null && id != null && key.equals(ProtectionCategories.categoryOf(g))) ids.add(id);
                    }
                }
                IdentraData.unassign(this, app, ids, this::onResult);
            }
            renderLater();
        });
        card.findViewById(R.id.identraCatBody).setOnClickListener(v -> sw.toggle());
        parent.addView(card);
    }

    // ---- 0.6.0: Found in this app ---------------------------------------------------

    private void loadFound(final boolean forceScan) {
        if (app == null || foundLoading) return;
        foundLoading = true;
        final Context ctx = getApplicationContext();
        final AppXpPacket a = app;
        if (forceScan) dev.identra.app.found.FoundStore.forgetScan(ctx, a.packageName);
        renderFound();
        new Thread(() -> {
            dev.identra.app.found.FoundStore.Found f = null;
            int added = 0;
            try {
                f = dev.identra.app.found.FoundStore.load(ctx, a.uid, a.packageName, forceScan);
                added = dev.identra.app.found.FoundIdentity.sync(ctx, a);
            } catch (Throwable t) {
                Log.w(TAG, "found: " + t);
            }
            final dev.identra.app.found.FoundStore.Found ff = f;
            final int fAdded = added;
            main.post(() -> {
                foundLoading = false;
                if (isFinishing()) return;
                found = ff;
                if (fAdded > 0) reload(); else renderFound();
            });
        }, "identra-found-ui").start();
    }

    private void renderFound() {
        if (foundBox == null || app == null) return;
        if (found == null && !foundLoading) { loadFound(false); return; }
        foundBox.removeAllViews();
        TextView head = (TextView) LayoutInflater.from(this).inflate(R.layout.item_identra_section, foundBox, false);
        head.setText(R.string.identra_found_title);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) head.getLayoutParams();
        if (lp != null) { lp.topMargin = IdentraRows.dp(this, 8); head.setLayoutParams(lp); }
        foundBox.addView(head);

        LinearLayout card = IdentraRows.card(foundBox);
        final boolean on = dev.identra.app.found.FoundStore.isIdentityOn(this, app.packageName, app.uid);
        String sub;
        if (on) {
            dev.identra.app.ui.profiles.Profile p = dev.identra.app.found.FoundIdentity.profileFor(this, app);
            int n = dev.identra.app.found.FoundStore.managed(this, app.packageName, app.uid).size();
            sub = getString(R.string.identra_found_use_all_on, n, p == null ? "-" : p.name);
        } else {
            sub = getString(R.string.identra_found_use_all_off);
        }
        MaterialSwitch sw = IdentraRows.switchRow(card, R.drawable.ic_identra_shield, getString(R.string.identra_found_use_all), sub, on,
                (b, checked) -> setIdentityForFound(checked));
        sw.setEnabled(!foundBusy && found != null);

        if (found == null) {
            IdentraRows.row(card, R.drawable.ic_identra_search, getString(R.string.identra_found_scanning), null, null);
            return;
        }
        int seen = found.seenCount();
        int likely = found.ids.size() - seen;
        if (found.ids.isEmpty()) {
            IdentraRows.row(card, R.drawable.ic_identra_info, getString(R.string.identra_found_empty), null, null);
        } else {
            IdentraRows.row(card, R.drawable.ic_identra_info, getString(R.string.identra_found_summary, seen, likely),
                    getString(R.string.identra_found_seen_desc) + "\n" + getString(R.string.identra_found_likely_desc), null);
        }

        // grouped by the same categories as the protections below
        Map<String, List<String>> byCat = new LinkedHashMap<>();
        dev.identra.app.found.FoundIndex index = dev.identra.app.found.FoundIndex.get();
        for (String id : found.ids.keySet()) {
            dev.identra.app.found.FoundIndex.Entry e = index.byId.get(id);
            if (e == null) continue;
            String cat = ProtectionCategories.categoryOf(e.group);
            List<String> l = byCat.get(cat);
            if (l == null) { l = new ArrayList<>(); byCat.put(cat, l); }
            l.add(id);
        }
        List<ProtectionCategories.Category> cats = new ArrayList<>(ProtectionCategories.ALL);
        cats.add(ProtectionCategories.OTHER_CATEGORY);
        for (final ProtectionCategories.Category c : cats) {
            List<String> ids = byCat.get(c.key);
            if (ids == null || ids.isEmpty()) continue;
            int cs = 0;
            for (String id : ids) if (Boolean.TRUE.equals(found.ids.get(id))) cs++;
            final boolean open = foundExpanded.contains(c.key);
            View row = IdentraRows.row(card, c.icon, getString(c.title), getString(R.string.identra_found_cat_sub, cs, ids.size() - cs), v -> {
                if (!foundExpanded.remove(c.key)) foundExpanded.add(c.key);
                renderFound();
            });
            row.findViewById(R.id.identraRowChevron).setRotation(open ? 90f : 0f);
            if (open) {
                // seen first
                List<String> sorted = new ArrayList<>(ids);
                java.util.Collections.sort(sorted, (x, y) -> Boolean.compare(!Boolean.TRUE.equals(found.ids.get(x)), !Boolean.TRUE.equals(found.ids.get(y))));
                for (String id : sorted) {
                    dev.identra.app.found.FoundIndex.Entry e = index.byId.get(id);
                    boolean wasSeen = Boolean.TRUE.equals(found.ids.get(id));
                    String title = friendlyHook(id);
                    String tag = getString(wasSeen ? R.string.identra_found_seen : R.string.identra_found_likely);
                    View r = IdentraRows.row(card, 0, "   " + title, tag + " · " + IdentraData.groupTitle(this, e.group), null);
                    r.setAlpha(wasSeen ? 1f : 0.75f);
                }
            }
        }
        IdentraRows.row(card, R.drawable.ic_identra_restart, getString(R.string.identra_found_rescan),
                getString(R.string.identra_found_rescan_desc), v -> loadFound(true));
    }

    private static String friendlyHook(String id) {
        String n = id.startsWith("Identra.") ? id.substring(8) : id;
        return n.replace('/', ' ').replace("  ", " ").trim();
    }

    private void setIdentityForFound(final boolean on) {
        if (app == null || foundBusy) return;
        foundBusy = true;
        final Context ctx = getApplicationContext();
        final AppXpPacket a = app;
        new Thread(() -> {
            dev.identra.app.found.FoundIdentity.Result r;
            try {
                r = on ? dev.identra.app.found.FoundIdentity.enable(ctx, a) : dev.identra.app.found.FoundIdentity.disable(ctx, a);
            } catch (Throwable t) {
                r = new dev.identra.app.found.FoundIdentity.Result();
                r.ok = false;
                r.error = String.valueOf(t);
            }
            final dev.identra.app.found.FoundIdentity.Result fr = r;
            main.post(() -> {
                foundBusy = false;
                if (isFinishing()) return;
                if (fr.noProfile) {
                    snack(getString(R.string.identra_found_no_profile));
                    renderFound();
                    dev.identra.app.ui.profiles.ProfileUi.chooseForApp(this, a, () -> {
                        reload();
                        if (dev.identra.app.found.FoundIdentity.profileFor(this, a) != null) setIdentityForFound(true);
                    });
                    return;
                }
                if (!fr.ok) snack(getString(R.string.identra_change_failed, String.valueOf(fr.error)));
                else if (on) {
                    String msg = getString(R.string.identra_found_done_on, fr.assigned);
                    if (fr.skippedNoValue > 0) msg += "\n" + getString(R.string.identra_found_skipped, fr.skippedNoValue);
                    if (fr.skippedNotIdentity > 0) msg += "\n" + getString(R.string.identra_found_not_identity);
                    snack(msg);
                } else snack(getString(R.string.identra_found_done_off, fr.removed));
                reload();
            });
        }, "identra-found-switch").start();
    }

    /** Re-render after the current click/animation finishes. */
    private void renderLater() {
        main.post(this::render);
    }

    private void onResult(final A_CODE code) {
        main.post(() -> {
            if (isFinishing()) return;
            if (!A_CODE.isSuccessful(code)) {
                snack(getString(R.string.identra_change_failed, String.valueOf(code)));
                reload();
            }
        });
    }

    // ---- LSPosed scope: tell the user when this app is not hooked, and offer one-tap hooking ----
    private boolean scopeRequestRunning;

    private static final String LSPD_CLI = "/data/adb/lspd/cli";

    /**
     * Shown only for an app the user protects: at the top of the screen when it is protected but outside
     * Identra's scope, and as a prompt right when protection is switched on (prompt = true).
     */
    private void addScopeCard(LinearLayout parent, final boolean prompt) {
        if (app == null) return;
        final String target = app.packageName;
        final int userId = app.uid / 100000;
        final LinearLayout holder = parent;
        holder.removeAllViews();
        if (!prompt && !IdentraData.isProtected(app)) return;
        IdentraData.EXECUTOR.submit(() -> {
            final io.github.libxposed.service.XposedService s = dev.identra.app.reload.HotReloader.service();
            final boolean root = hasRoot();
            Boolean scoped = null;
            if (root) scoped = cliScoped(target, userId);
            if (scoped == null && s != null) {
                try { List<String> sc = s.getScope(); scoped = sc != null && sc.contains(target); }
                catch (Throwable t) { Log.w(TAG, "getScope: " + t); }
            }
            final Boolean fScoped = scoped;
            Log.i(TAG, "scope check pkg=" + target + " user=" + userId + " root=" + root + " service=" + (s != null) + " scoped=" + scoped);
            main.post(() -> {
                if (isFinishing() || app == null || !target.equals(app.packageName)) return;
                holder.removeAllViews();
                if (Boolean.TRUE.equals(fScoped)) return;
                if (prompt && fScoped != null) {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.identra_scope_missing_title)
                            .setMessage(R.string.identra_scope_prompt_msg)
                            .setPositiveButton(R.string.identra_scope_hook_now_short, (d, w) -> {
                                if (root) hookWithRoot(target, userId, null);
                                else if (s != null) requestHook(s, target, null);
                            })
                            .setNegativeButton(R.string.identra_scope_later, null)
                            .show();
                }
                LinearLayout card = IdentraRows.card(holder);
                if (fScoped == null) {
                    IdentraRows.row(card, R.drawable.ic_identra_info, getString(R.string.identra_scope_noservice_title),
                            getString(R.string.identra_scope_noservice_sub), null);
                    return;
                }
                IdentraRows.row(card, R.drawable.ic_identra_info, getString(R.string.identra_scope_missing_title),
                        getString(R.string.identra_scope_missing_sub), null);
                final View[] btn = new View[1];
                btn[0] = IdentraRows.row(card, R.drawable.ic_identra_shield, getString(R.string.identra_scope_hook_now),
                        getString(R.string.identra_scope_hook_now_sub), v -> {
                            if (root) hookWithRoot(target, userId, btn[0]);
                            else if (s != null) requestHook(s, target, btn[0]);
                        });
            });
        });
    }

    private static boolean hasRoot() {
        try { return Boolean.TRUE.equals(com.topjohnwu.superuser.Shell.isAppGrantedRoot())
                || com.topjohnwu.superuser.Shell.getShell().isRoot(); }
        catch (Throwable t) { return false; }
    }

    /** Vector/LSPosed CLI: is pkg/user in Identra's scope? null when the CLI is unavailable. */
    private static Boolean cliScoped(String target, int userId) {
        try {
            com.topjohnwu.superuser.Shell.Result r = com.topjohnwu.superuser.Shell
                    .cmd(LSPD_CLI + " --json scope ls dev.identra.app").exec();
            String out = String.join("\n", r.getOut());
            if (!r.isSuccess() || !out.contains("\"success\"")) { Log.w(TAG, "cli ls failed: " + out); return null; }
            org.json.JSONObject o = new org.json.JSONObject(out.substring(out.indexOf('{')));
            org.json.JSONArray data = o.optJSONArray("data");
            if (data == null) return null;
            for (int i = 0; i < data.length(); i++) {
                org.json.JSONObject e = data.getJSONObject(i);
                if (target.equals(e.optString("APP_PACKAGE")) && e.optInt("USER_ID", 0) == userId) return true;
            }
            return false;
        } catch (Throwable t) {
            Log.w(TAG, "cli ls: " + t);
            return null;
        }
    }

    private void hookWithRoot(final String target, final int userId, final View btn) {
        if (scopeRequestRunning) return;
        if (!target.matches("[A-Za-z0-9._]+")) return;
        scopeRequestRunning = true;
        if (btn != null) IdentraRows.setSub(btn, getString(R.string.identra_scope_working));
        IdentraData.EXECUTOR.submit(() -> {
            String err = null;
            boolean ok = false, stopped = false;
            try {
                com.topjohnwu.superuser.Shell.Result r = com.topjohnwu.superuser.Shell
                        .cmd(LSPD_CLI + " scope add dev.identra.app " + target + "/" + userId).exec();
                ok = Boolean.TRUE.equals(cliScoped(target, userId));
                if (!ok) err = String.join(" ", r.getOut()) + " " + String.join(" ", r.getErr());
                if (ok) stopped = com.topjohnwu.superuser.Shell.cmd("am force-stop --user " + userId + " " + target).exec().isSuccess();
            } catch (Throwable t) { err = String.valueOf(t); }
            Log.i(TAG, "root scope add pkg=" + target + "/" + userId + " ok=" + ok + " stopped=" + stopped + " err=" + err);
            final boolean fOk = ok, fStopped = stopped;
            final String fErr = err;
            main.post(() -> {
                scopeRequestRunning = false;
                if (isFinishing()) return;
                snack(fOk ? getString(fStopped ? R.string.identra_scope_done_restarted : R.string.identra_scope_done)
                        : getString(R.string.identra_scope_failed, String.valueOf(fErr).trim()));
                render();
            });
        });
    }

    private void requestHook(final io.github.libxposed.service.XposedService s, final String target, final View btn) {
        if (scopeRequestRunning) return;
        scopeRequestRunning = true;
        if (btn != null) IdentraRows.setSub(btn, getString(R.string.identra_scope_waiting));
        try {
            List<String> req = new ArrayList<>();
            req.add(target);
            s.requestScope(req, new io.github.libxposed.service.XposedService.OnScopeEventListener() {
                @Override public void onScopeRequestApproved(List<String> approved) {
                    final boolean ok = approved != null && approved.contains(target);
                    boolean stopped = false;
                    if (ok) stopped = forceStopWithRoot(target);
                    final boolean fStopped = stopped;
                    main.post(() -> {
                        scopeRequestRunning = false;
                        if (isFinishing()) return;
                        snack(getString(ok ? (fStopped ? R.string.identra_scope_done_restarted : R.string.identra_scope_done)
                                : R.string.identra_scope_denied));
                        render();
                    });
                }
                @Override public void onScopeRequestFailed(String message) {
                    main.post(() -> {
                        scopeRequestRunning = false;
                        if (isFinishing()) return;
                        snack(getString(R.string.identra_scope_failed, String.valueOf(message)));
                        render();
                    });
                }
            });
        } catch (Throwable t) {
            scopeRequestRunning = false;
            snack(getString(R.string.identra_scope_failed, String.valueOf(t.getMessage())));
            render();
        }
    }

    /** Root only: stop the app so its next launch loads Identra's hooks. Runs off the main thread (binder callback). */
    private static boolean forceStopWithRoot(String target) {
        try {
            if (!Boolean.TRUE.equals(com.topjohnwu.superuser.Shell.isAppGrantedRoot())) return false;
            if (!target.matches("[A-Za-z0-9._]+")) return false;
            return com.topjohnwu.superuser.Shell.cmd("am force-stop " + target).exec().isSuccess();
        } catch (Throwable t) {
            Log.w(TAG, "force-stop: " + t);
            return false;
        }
    }

    private void snack(String text) {
        Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_LONG).show();
    }

    @SuppressWarnings("unused")
    private static int count(Collection<?> c) { return c == null ? 0 : c.size(); }
}
