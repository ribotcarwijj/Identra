package dev.identra.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import dev.identra.app.BuildConfig;
import dev.identra.app.R;
import dev.identra.app.ui.tools.IdentraBackupActivity;
import dev.identra.app.ui.tools.IdentraHooksActivity;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.ui.core.UserClientAppContext;

/** Home tab: status hero, stat tiles, quick actions, recently changed apps. */
public class IdentraDashboardFragment extends Fragment {
    private final Handler main = new Handler(Looper.getMainLooper());
    private View root;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_identra_dashboard, container, false);

        quick(R.id.dashQuickProtect, R.drawable.ic_identra_shield, R.string.identra_dash_quick_protect, v -> home().selectTab(R.id.identra_nav_apps));
        quick(R.id.dashQuickLocation, R.drawable.ic_identra_location, R.string.identra_dash_quick_location, v -> home().selectTab(R.id.identra_nav_location));
        quick(R.id.dashQuickBackup, R.drawable.ic_identra_backup, R.string.identra_dash_quick_backup,
                v -> startActivity(new Intent(requireContext(), IdentraBackupActivity.class)));

        root.findViewById(R.id.dashBtnProtected).setOnClickListener(v -> home().selectTab(R.id.identra_nav_apps));
        root.findViewById(R.id.dashBtnInstances).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), dev.identra.app.ui.tools.IdentraInstancesActivity.class)));
        root.findViewById(R.id.dashTileApps).setOnClickListener(v -> home().selectTab(R.id.identra_nav_apps));
        root.findViewById(R.id.dashTileLocation).setOnClickListener(v -> home().selectTab(R.id.identra_nav_location));
        root.findViewById(R.id.dashTileProfile).setOnClickListener(v -> home().selectTab(R.id.identra_nav_profiles));
        root.findViewById(R.id.dashTileCollections).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), IdentraHooksActivity.class)));

        ((TextView) root.findViewById(R.id.dashApi)).setText(R.string.identra_dash_api);
        ((TextView) root.findViewById(R.id.dashVersion)).setText(getString(R.string.identra_dash_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        onStatus();
        return root;
    }

    private HomeActivity home() { return (HomeActivity) requireActivity(); }

    private void quick(int id, int icon, int label, View.OnClickListener l) {
        View q = root.findViewById(id);
        ((ImageView) q.findViewById(R.id.quickIcon)).setImageResource(icon);
        ((TextView) q.findViewById(R.id.quickLabel)).setText(label);
        q.setOnClickListener(l);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) refresh();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refresh();
    }

    private void refresh() {
        renderLocation();
        renderProfiles();
        renderRecent();
        IdentraData.refresh(requireContext(), s -> {
            if (!isAdded() || root == null) return;
            renderCounts(s);
            renderRecent();
        });
        // cheap check of the global location the module actually holds
        final Context app = requireContext().getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            final IdentraLocation.Value v = IdentraLocation.readGlobal(app);
            main.post(() -> { if (isAdded()) renderLocation(v); });
        });
    }

    /** Called by HomeActivity when the module status check finishes. */
    public void onStatus() {
        if (root == null || !isAdded()) return;
        HomeActivity.Status st = home().status();
        StatusPulseView dot = root.findViewById(R.id.dashDot);
        TextView fw = root.findViewById(R.id.dashFramework);
        TextView rootLine = root.findViewById(R.id.dashRoot);
        TextView title = root.findViewById(R.id.dashStatusTitle);
        TextView body = root.findViewById(R.id.dashStatusBody);
        TextView bridge = root.findViewById(R.id.dashBridge);
        View banner = root.findViewById(R.id.dashBanner);
        if (!st.checked) {
            dot.setState(StatusPulseView.CHECKING);
            title.setText(R.string.identra_dash_checking);
            body.setText(null);
            fw.setText(null);
            rootLine.setText(null);
            bridge.setText(null);
            banner.setVisibility(View.GONE);
            return;
        }
        boolean noRoot = st.root != null && !st.root;
        fw.setText(st.available ? getString(R.string.identra_dash_fw_ok) : getString(R.string.identra_dash_fw_off));
        fw.setTextColor(st.available ? 0xFF2E9E5B : 0xFFD84343);
        rootLine.setText(noRoot ? getString(R.string.identra_dash_root_missing) : getString(R.string.identra_dash_root_ok));
        rootLine.setTextColor(noRoot ? 0xFFD84343 : 0xFF2E9E5B);
        if (st.message == 0 && !noRoot) {
            dot.setState(StatusPulseView.OK);
            title.setText(R.string.identra_dash_active);
            body.setText(R.string.identra_dash_active_body);
            banner.setVisibility(View.GONE);
        } else if (st.message == 0) {
            // module fine, root missing
            dot.setState(StatusPulseView.WARN);
            title.setText(R.string.identra_dash_attention);
            body.setText(R.string.identra_dash_root_missing_body);
            banner.setVisibility(View.GONE);
        } else {
            dot.setState(st.available ? StatusPulseView.WARN : StatusPulseView.OFF);
            title.setText(st.available ? R.string.identra_dash_attention : R.string.identra_dash_inactive);
            body.setText(st.message);
            ((TextView) root.findViewById(R.id.dashBannerText)).setText(st.message);
            Button action = root.findViewById(R.id.dashBannerAction);
            final Intent lsposed = st.offerLsposed ? requireContext().getPackageManager().getLaunchIntentForPackage("org.lsposed.manager") : null;
            if (lsposed != null) {
                action.setVisibility(View.VISIBLE);
                action.setOnClickListener(v -> { try { startActivity(lsposed); } catch (Exception ignored) { } });
            } else {
                action.setVisibility(View.GONE);
            }
            banner.setVisibility(View.VISIBLE);
        }
        bridge.setText(st.bridge == null ? getString(R.string.identra_dash_bridge_none) : getString(R.string.identra_dash_bridge, st.bridge));
    }

    private void renderCounts(IdentraData.Snapshot s) {
        TextView apps = root.findViewById(R.id.dashAppsValue);
        TextView cols = root.findViewById(R.id.dashCollectionsValue);
        if (s == null) {
            apps.setText(R.string.identra_dash_dash);
            cols.setText(R.string.identra_dash_dash);
            return;
        }
        int prot = IdentraData.protectedApps(s).size();
        apps.setText(String.valueOf(prot));
        ((TextView) root.findViewById(R.id.dashAppsLabel)).setText(prot == 1 ? R.string.identra_dash_protected_app : R.string.identra_dash_protected_apps);
        cols.setText(String.valueOf(s.hooks.size()));
    }

    /** 0.4.0: global default profile and how many apps have one. */
    private void renderProfiles() {
        dev.identra.app.ui.profiles.ProfileStore store = dev.identra.app.ui.profiles.ProfileStore.get(requireContext());
        dev.identra.app.ui.profiles.Profile g = store.global();
        TextView name = root.findViewById(R.id.dashProfileName);
        TextView sub = root.findViewById(R.id.dashProfileSub);
        int apps = store.allAssignments().size();
        int count = store.count();
        if (count == 0) {
            name.setText(R.string.identra_prof_empty_title);
            sub.setText(R.string.identra_dash_profiles_none);
            return;
        }
        name.setText(g == null ? getString(R.string.identra_prof_global_none) : getString(R.string.identra_dash_profile_default, g.name));
        sub.setText(getString(R.string.identra_dash_profiles_sub, count, apps));
    }

    private void renderLocation() {
        IdentraPlaces.Place p = IdentraPlaces.appliedGlobal(requireContext());
        TextView name = root.findViewById(R.id.dashLocationName);
        TextView coords = root.findViewById(R.id.dashLocationCoords);
        if (p == null) {
            name.setText(R.string.identra_loc_not_set);
            coords.setVisibility(View.GONE);
        } else {
            name.setText(p.name);
            coords.setText(IdentraPlaces.shortCoords(p.lat, p.lon));
            coords.setVisibility(View.VISIBLE);
        }
    }

    private void renderLocation(IdentraLocation.Value v) {
        TextView name = root.findViewById(R.id.dashLocationName);
        TextView coords = root.findViewById(R.id.dashLocationCoords);
        if (v == null || !v.set) {
            name.setText(R.string.identra_loc_not_set);
            coords.setVisibility(View.GONE);
            return;
        }
        String n = IdentraPlaces.nameFor(requireContext(), null, 0, v.lat, v.lon);
        name.setText(n != null ? n : getString(R.string.identra_loc_custom_point));
        coords.setText(IdentraPlaces.shortCoords(v.lat, v.lon));
        coords.setVisibility(View.VISIBLE);
    }

    private void renderRecent() {
        LinearLayout box = root.findViewById(R.id.dashRecent);
        box.removeAllViews();
        final Context c = requireContext();
        List<IdentraData.RecentChange> list = IdentraData.recentChanges(c);
        View empty = root.findViewById(R.id.dashRecentEmpty);
        root.findViewById(R.id.dashRecentCard).setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        if (list.isEmpty()) {
            ((TextView) empty.findViewById(R.id.identraEmptyTitle)).setText(R.string.identra_dash_recent_empty);
            TextView b = empty.findViewById(R.id.identraEmptyBody);
            b.setText(R.string.identra_dash_recent_empty_body);
            b.setVisibility(View.VISIBLE);
            return;
        }
        IdentraData.Snapshot s = IdentraData.cached();
        int shown = 0;
        for (final IdentraData.RecentChange r : list) {
            if (shown++ >= 5) break;
            final AppXpPacket app = IdentraData.findApp(s, r.packageName, r.uid);
            boolean on = app != null ? IdentraData.isProtected(app) : r.protectedNow;
            String when = DateUtils.getRelativeTimeSpanString(r.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
            dev.identra.app.ui.profiles.Profile rp = dev.identra.app.ui.profiles.ProfileStore.get(c).profileFor(r.packageName, r.uid);
            String sub = getString(on ? R.string.identra_detail_protected : R.string.identra_detail_off)
                    + (rp != null ? " · " + getString(R.string.identra_status_profile, rp.name) : "") + " · " + when;
            View row = IdentraRows.row(box, 0, app != null ? IdentraData.labelOf(app) : r.label, sub,
                    app == null ? null : (View.OnClickListener) v -> startActivity(AppDetailActivity.intent(c, app)));
            ImageView icon = row.findViewById(R.id.identraRowIcon);
            icon.setVisibility(View.VISIBLE);
            if (app == null || app.icon <= 0) icon.setImageResource(android.R.drawable.sym_def_app_icon);
            else UserClientAppContext.attachIcon(c, IdentraRows.dp(c, 24), icon, app.packageName, app.icon);
        }
    }
}
