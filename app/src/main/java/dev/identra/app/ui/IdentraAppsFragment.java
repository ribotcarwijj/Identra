package dev.identra.app.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.identra.app.R;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;

public class IdentraAppsFragment extends Fragment implements IdentraAppsAdapter.Listener {
    private static final String TAG = "Identra.UiApps";
    private static final String PREF_FILTER = "identra_ui_apps_filter";

    private static final int FILTER_ALL = 0, FILTER_PROTECTED = 1, FILTER_USER = 2, FILTER_SYSTEM = 3;

    private final Handler main = new Handler(Looper.getMainLooper());
    private IdentraAppsAdapter adapter;
    private SwipeRefreshLayout swipe;
    private CircularProgressIndicator progress;
    private TextView empty;
    private View emptyBox;
    private EditText search;
    private ChipGroup chips;
    private int filter = FILTER_USER;
    private String query = "";
    private boolean receiverRegistered = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_identra_apps, container, false);
        swipe = v.findViewById(R.id.identraSwipe);
        progress = v.findViewById(R.id.identraAppsProgress);
        empty = v.findViewById(R.id.identraAppsEmpty);
        emptyBox = v.findViewById(R.id.identraAppsEmptyBox);
        search = v.findViewById(R.id.identraSearch);
        chips = v.findViewById(R.id.identraChips);

        RecyclerView rv = v.findViewById(R.id.identraAppList);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new IdentraAppsAdapter(requireContext(), this);
        rv.setAdapter(adapter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        filter = prefs.getInt(PREF_FILTER, FILTER_USER);
        chips.check(chipFor(filter));
        chips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            filter = filterFor(ids.get(0));
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putInt(PREF_FILTER, filter).apply();
            applyFilter();
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                query = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                applyFilter();
            }
        });

        swipe.setOnRefreshListener(this::reload);

        IdentraData.Snapshot cached = IdentraData.cached();
        if (cached != null) {
            progress.setVisibility(View.GONE);
            applyFilter();
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!receiverRegistered) {
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_PACKAGE_ADDED);
            f.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
            f.addDataScheme("package");
            requireContext().registerReceiver(packageReceiver, f);
            receiverRegistered = true;
        }
        // show what we have (detail screen may have changed it), then refresh from the service
        if (IdentraData.cached() != null) applyFilter();
        reload();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (receiverRegistered) {
            try { requireContext().unregisterReceiver(packageReceiver); } catch (Exception ignored) { }
            receiverRegistered = false;
        }
    }

    public void reload() {
        final Context ctx = requireContext().getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            boolean ok = true;
            boolean stale = false;
            try {
                stale = IdentraData.load(ctx).stale;
            } catch (Throwable t) {
                Log.e(TAG, "Load failed: " + t);
                ok = false;
            }
            final boolean fOk = ok;
            final boolean fStale = stale;
            main.post(() -> {
                if (!isAdded() || getView() == null) return;
                swipe.setRefreshing(false);
                progress.setVisibility(View.GONE);
                if (!fOk && IdentraData.cached() == null) {
                    empty.setText(R.string.identra_apps_loading_failed);
                    emptyBox.setVisibility(View.VISIBLE);
                    return;
                }
                if (fStale) { reload(); return; }
                applyFilter();
            });
        });
    }

    private void applyFilter() {
        IdentraData.Snapshot s = IdentraData.cached();
        if (s == null || adapter == null) return;
        List<AppXpPacket> out = new ArrayList<>();
        for (AppXpPacket app : s.apps) {
            if (!query.isEmpty()) {
                String label = app.label == null ? "" : app.label.toLowerCase(Locale.ROOT);
                if (!label.contains(query) && !app.packageName.toLowerCase(Locale.ROOT).contains(query)) continue;
            }
            switch (filter) {
                case FILTER_PROTECTED:
                    if (!IdentraData.isProtected(app)) continue;
                    break;
                case FILTER_USER:
                    if (!(app.uid > Process.FIRST_APPLICATION_UID && app.enabled && !app.system)) continue;
                    break;
                case FILTER_SYSTEM:
                    if (!app.system) continue;
                    break;
                default:
                    break;
            }
            out.add(app);
        }
        adapter.submit(s, out);
        empty.setText(R.string.identra_apps_empty);
        emptyBox.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onOpen(AppXpPacket app) {
        startActivity(AppDetailActivity.intent(requireContext(), app));
    }

    @Override
    public void onToggle(final AppXpPacket app, boolean on, final MaterialSwitch view) {
        final IdentraData.Snapshot s = IdentraData.cached();
        final Context ctx = requireContext();
        boolean started;
        if (on) {
            List<XHook> hooks = IdentraData.defaultHooks(s, app);
            started = IdentraData.assign(ctx, app, hooks, code -> onResult(code));
            if (!started && hooks.isEmpty()) {
                view.post(() -> view.setChecked(false));
                snack(getString(R.string.identra_no_protections));
                return;
            }
        } else {
            started = IdentraData.unassignAll(ctx, app, code -> onResult(code));
        }
        // refresh row status (and the Protected filter)
        view.post(this::applyFilter);
    }

    private void onResult(final A_CODE code) {
        main.post(() -> {
            if (!isAdded()) return;
            if (!A_CODE.isSuccessful(code)) {
                snack(getString(R.string.identra_change_failed, String.valueOf(code)));
                reload();
            }
        });
    }

    private void snack(String text) {
        View v = getView();
        if (v != null) Snackbar.make(v, text, Snackbar.LENGTH_LONG).show();
    }

    private static int chipFor(int filter) {
        switch (filter) {
            case FILTER_ALL: return R.id.identraChipAll;
            case FILTER_PROTECTED: return R.id.identraChipProtected;
            case FILTER_SYSTEM: return R.id.identraChipSystem;
            default: return R.id.identraChipUser;
        }
    }

    private static int filterFor(int chipId) {
        if (chipId == R.id.identraChipAll) return FILTER_ALL;
        if (chipId == R.id.identraChipProtected) return FILTER_PROTECTED;
        if (chipId == R.id.identraChipSystem) return FILTER_SYSTEM;
        return FILTER_USER;
    }

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) reload();
        }
    };
}
