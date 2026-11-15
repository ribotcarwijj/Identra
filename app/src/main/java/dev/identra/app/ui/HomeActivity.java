package dev.identra.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

import dev.identra.app.ActivityMain;
import dev.identra.app.R;
import dev.identra.app.api.core.provider.IdentraHookProvider;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.call.GetBridgeVersionCommand;
import dev.identra.app.x.core.commands.call.GetDatabaseStatusCommand;

/** Launcher (0.3.0): bottom tabs Home / Apps / Location / Profiles / Settings. */
public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "Identra.UiHome";
    private static final String STATE_TAB = "identra.tab";
    private static final String TAG_HOME = "identra.home", TAG_APPS = "identra.apps", TAG_LOCATION = "identra.location",
            TAG_PROFILES = "identra.profiles", TAG_SETTINGS = "identra.settings";
    private static final String[] TAGS = {TAG_HOME, TAG_APPS, TAG_LOCATION, TAG_PROFILES, TAG_SETTINGS};

    /** Extras to open a tab (e.g. Location for one app from the app detail screen). */
    public static final String EXTRA_TAB = "identra.open_tab";
    public static final String EXTRA_TARGET_PKG = "identra.target_pkg";
    public static final String EXTRA_TARGET_UID = "identra.target_uid";
    public static final String TAB_LOCATION = "location";

    public static Intent locationFor(android.content.Context context, String pkg, int uid) {
        Intent i = new Intent(context, HomeActivity.class);
        i.putExtra(EXTRA_TAB, TAB_LOCATION);
        i.putExtra(EXTRA_TARGET_PKG, pkg);
        i.putExtra(EXTRA_TARGET_UID, uid);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return i;
    }

    /** Module status, shared with the Home dashboard. */
    public static final class Status {
        public boolean checked;
        public boolean available;
        public int message;
        public boolean offerLsposed;
        public String bridge;
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Status status = new Status();
    private int currentTab = R.id.identra_nav_home;
    private AlertDialog licenseDialog;
    private BottomNavigationView nav;

    public Status status() { return status; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IdentraTheme.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identra_home);

        if (savedInstanceState != null) currentTab = savedInstanceState.getInt(STATE_TAB, currentTab);

        nav = findViewById(R.id.identraBottomNav);
        nav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        nav.setOnItemReselectedListener(item -> { /* stay */ });
        nav.setSelectedItemId(currentTab);
        showTab(currentTab);
        handleOpen(getIntent());

        // 0.7.20: back on any tab other than Home goes to Home first, instead of leaving the app
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else if (currentTab != R.id.identra_nav_home) {
                    selectTab(R.id.identra_nav_home);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        checkService();
        checkFirstRun();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpen(intent);
    }

    private void handleOpen(Intent intent) {
        if (intent == null || !TAB_LOCATION.equals(intent.getStringExtra(EXTRA_TAB))) return;
        String pkg = intent.getStringExtra(EXTRA_TARGET_PKG);
        int uid = intent.getIntExtra(EXTRA_TARGET_UID, -1);
        intent.removeExtra(EXTRA_TAB);
        selectTab(R.id.identra_nav_location);
        Fragment f = getSupportFragmentManager().findFragmentByTag(TAG_LOCATION);
        if (f instanceof IdentraLocationFragment && pkg != null) ((IdentraLocationFragment) f).setTarget(pkg, uid);
    }

    /** Switch tabs programmatically (quick actions, tiles). */
    public void selectTab(int id) {
        if (nav != null && nav.getSelectedItemId() != id) nav.setSelectedItemId(id);
        else showTab(id);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, currentTab);
    }

    private static String tagFor(int id) {
        if (id == R.id.identra_nav_apps) return TAG_APPS;
        if (id == R.id.identra_nav_location) return TAG_LOCATION;
        if (id == R.id.identra_nav_profiles) return TAG_PROFILES;
        if (id == R.id.identra_nav_settings) return TAG_SETTINGS;
        return TAG_HOME;
    }

    private void showTab(int id) {
        boolean changed = currentTab != id;
        currentTab = id;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        String want = tagFor(id);
        Fragment shown = null;
        for (String t : TAGS) {
            Fragment f = fm.findFragmentByTag(t);
            if (t.equals(want)) {
                if (f == null) { f = create(t); tx.add(R.id.identraContent, f, t); }
                else tx.show(f);
                shown = f;
            } else if (f != null && !f.isHidden()) {
                tx.hide(f);
            }
        }
        tx.commitNowAllowingStateLoss();
        // calm fade-through between tabs
        if (changed && shown != null && shown.getView() != null) {
            View v = shown.getView();
            v.setAlpha(0f);
            v.setTranslationY(IdentraRows.dp(this, 8));
            v.animate().alpha(1f).translationY(0f).setDuration(200)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
        updateBanner();
    }

    private static Fragment create(String tag) {
        switch (tag) {
            case TAG_APPS: return new IdentraAppsFragment();
            case TAG_LOCATION: return new IdentraLocationFragment();
            case TAG_PROFILES: return new IdentraProfilesFragment();
            case TAG_SETTINGS: return new IdentraSettingsFragment();
            default: return new IdentraDashboardFragment();
        }
    }

    /** Called after the Collections dialog closes. */
    public void onCollectionsChanged() {
        Fragment f = getSupportFragmentManager().findFragmentByTag(TAG_APPS);
        if (f instanceof IdentraAppsFragment && f.isAdded()) ((IdentraAppsFragment) f).reload();
    }

    // ---- module status: calm inline banner instead of a blocking dialog ----

    /** One automatic hot reload attempt per app run. */
    private static boolean reloadTried = false;

    private void hotReload() {
        dev.identra.app.reload.HotReloader.reloadAll(out -> {
            if (isFinishing()) return;
            android.util.Log.i("Identra.Home", "hot reload: system " + out.systemOk + "/" + out.systemTargets
                    + " apps " + out.appOk + "/" + out.appTargets + (out.message != null ? " " + out.message : "") + (out.noService ? " (no framework service)" : ""));
            if (out.systemReloaded()) {
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content),
                        R.string.identra_reloaded, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            }
            checkService();
        });
    }

    private void checkService() {
        IdentraData.EXECUTOR.submit(() -> {
            int message = 0;
            boolean offerLsposed = false;
            try {
                if (!IdentraHookProvider.isAvailable(this)) {
                    message = R.string.identra_banner_unavailable;
                    offerLsposed = true;
                } else if (dev.identra.app.x.core.commands.call.GetBuildCommand.get(this) < dev.identra.app.BuildConfig.VERSION_CODE) {
                    // 0.6.1: the system part runs an older build -> hot reload it (never a restart)
                    if (!reloadTried) {
                        reloadTried = true;
                        message = R.string.identra_banner_updating;
                        main.post(this::hotReload);
                    } else {
                        message = R.string.identra_banner_system_older;
                    }
                } else {
                    String bridge = GetBridgeVersionCommand.get(this);
                    if (Str.isEmpty(bridge) || bridge.equalsIgnoreCase(GetBridgeVersionCommand.DEFAULT)) {
                        message = R.string.identra_banner_reboot;
                    } else {
                        Pair<Integer, String> db = GetDatabaseStatusCommand.get(this);
                        if (db != null && db.first == GetDatabaseStatusCommand.CODE_ERROR)
                            message = R.string.identra_banner_database;
                        else
                            ActivityMain.cleanHooksOnVersionChange(this);
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Service check failed: " + t);
                message = R.string.identra_banner_unavailable;
                offerLsposed = true;
            }
            String bridgeVersion = null;
            try {
                String b = GetBridgeVersionCommand.get(this);
                if (!Str.isEmpty(b) && !b.equalsIgnoreCase(GetBridgeVersionCommand.DEFAULT)) bridgeVersion = b;
            } catch (Throwable ignored) { }
            final int fMessage = message;
            final boolean fOffer = offerLsposed;
            final String fBridge = bridgeVersion;
            main.post(() -> {
                status.checked = true;
                status.message = fMessage;
                status.offerLsposed = fOffer;
                status.available = !fOffer;
                status.bridge = fBridge;
                updateBanner();
                Fragment f = getSupportFragmentManager().findFragmentByTag(TAG_HOME);
                if (f instanceof IdentraDashboardFragment) ((IdentraDashboardFragment) f).onStatus();
            });
        });
    }

    /** The top banner is for the other tabs; Home shows the same message inline. */
    private void updateBanner() {
        if (!status.checked) return;
        showBanner(currentTab == R.id.identra_nav_home ? 0 : status.message, status.offerLsposed);
    }

    private void showBanner(int message, boolean offerLsposed) {
        if (isFinishing()) return;
        View banner = findViewById(R.id.identraBanner);
        if (message == 0) {
            banner.setVisibility(View.GONE);
            return;
        }
        ((TextView) findViewById(R.id.identraBannerText)).setText(message);
        Button action = findViewById(R.id.identraBannerAction);
        final Intent lsposed = offerLsposed ? getPackageManager().getLaunchIntentForPackage("org.lsposed.manager") : null;
        if (lsposed != null) {
            action.setVisibility(View.VISIBLE);
            action.setOnClickListener(v -> {
                try { startActivity(lsposed); } catch (Exception ignored) { }
            });
        } else {
            action.setVisibility(View.GONE);
        }
        banner.setVisibility(View.VISIBLE);
    }

    // ---- same first-run license acceptance as the classic screen (same pref key) ----

    private void checkFirstRun() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("firstrun", true) || licenseDialog != null) return;
        try {
            View view = LayoutInflater.from(this).inflate(R.layout.license, null, false);
            TextView tv = view.findViewById(R.id.tvLicense);
            tv.setMovementMethod(LinkMovementMethod.getInstance());
            tv.setText(Html.fromHtml(getString(R.string.title_license, Calendar.getInstance().get(Calendar.YEAR))));
            licenseDialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .setPositiveButton(R.string.title_accept, (d, w) -> prefs.edit().putBoolean("firstrun", false).apply())
                    .setNegativeButton(R.string.title_deny, (d, w) -> finishAffinity())
                    .setOnDismissListener(d -> licenseDialog = null)
                    .create();
            licenseDialog.show();
        } catch (Throwable t) {
            Log.e(TAG, "License dialog failed: " + t);
        }
    }
}
