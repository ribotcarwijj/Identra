package dev.identra.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import dev.identra.app.BuildConfig;
import dev.identra.app.DebugUtil;
import dev.identra.app.R;
import dev.identra.app.ui.tools.IdentraBackupActivity;
import dev.identra.app.ui.tools.IdentraHelpActivity;
import dev.identra.app.ui.tools.IdentraHookEditorActivity;
import dev.identra.app.ui.tools.IdentraHooksActivity;
import dev.identra.app.ui.tools.IdentraLogActivity;
import dev.identra.app.ui.tools.IdentraValuesActivity;
import dev.identra.app.x.core.commands.call.GetSettingExCommand;
import dev.identra.app.x.core.commands.call.PutSettingExCommand;
import dev.identra.app.x.core.database.A_CODE;

public class IdentraSettingsFragment extends Fragment {
    private static final String TAG = "Identra.UiSettings";
    private final Handler main = new Handler(Looper.getMainLooper());

    private MaterialSwitch notifySwitch, verboseSwitch;
    private LinearLayout list;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_identra_settings, container, false);
        list = root.findViewById(R.id.identraSettingsContainer);
        rebuild();
        return root;
    }

    private void rebuild() {
        // keep the title (first child), rebuild everything below it
        while (list.getChildCount() > 1) list.removeViewAt(1);
        build(list);
        loadServiceValues();
    }

    private void build(LinearLayout list) {
        final Context ctx = requireContext();

        // ---- General ----
        IdentraRows.section(list, getString(R.string.identra_settings_general));
        LinearLayout general = IdentraRows.card(list);
        final boolean follow = IdentraTheme.followsSystem(ctx);
        IdentraRows.switchRow(general, R.drawable.ic_identra_palette, getString(R.string.identra_settings_follow_system),
                getString(R.string.identra_settings_follow_system_desc), follow, (b, on) -> {
                    IdentraTheme.prefs(ctx).edit().putBoolean(IdentraTheme.PREF_FOLLOW_SYSTEM, on).apply();
                    applyNightMode();
                });
        if (!follow) {
            IdentraRows.switchRow(general, R.drawable.ic_identra_palette, getString(R.string.identra_settings_dark), null,
                    IdentraTheme.isDark(ctx), (b, on) -> {
                        IdentraTheme.prefs(ctx).edit().putBoolean(IdentraTheme.PREF_DARK, on).apply();
                        // keep the classic screens in step (same internal "theme" setting as before)
                        final Context app = ctx.getApplicationContext();
                        IdentraData.EXECUTOR.submit(() -> PutSettingExCommand.putTheme(app, on ? "dark" : "light"));
                        applyNightMode();
                    });
        }
        notifySwitch = IdentraRows.switchRow(general, R.drawable.ic_identra_bell, getString(R.string.identra_settings_notify_new),
                getString(R.string.identra_settings_notify_new_desc), false, null);
        notifySwitch.setEnabled(false);
        verboseSwitch = IdentraRows.switchRow(general, R.drawable.ic_identra_bug, getString(R.string.identra_settings_verbose),
                getString(R.string.identra_settings_verbose_desc), false, null);
        verboseSwitch.setEnabled(false);
        IdentraRows.row(general, R.drawable.ic_identra_restart, getString(R.string.identra_adv_force_stop),
                getString(R.string.identra_settings_force_stop_note), null);

        // ---- Advanced (0.5.0: one Identra-style section; the classic screens are gone) ----
        IdentraRows.section(list, getString(R.string.identra_settings_tools));
        LinearLayout tools = IdentraRows.card(list);
        IdentraRows.row(tools, R.drawable.ic_identra_code, getString(R.string.identra_tool_hooks), getString(R.string.identra_tool_hooks_desc),
                v -> startActivity(new Intent(ctx, IdentraHooksActivity.class)));
        IdentraRows.row(tools, R.drawable.ic_identra_tune, getString(R.string.identra_tool_values), getString(R.string.identra_tool_values_desc),
                v -> startActivity(IdentraValuesActivity.global(ctx)));
        IdentraRows.row(tools, R.drawable.ic_identra_apps, getString(R.string.identra_tool_app_values), getString(R.string.identra_tool_app_values_desc),
                v -> IdentraAppPicker.show(ctx, getString(R.string.identra_tool_app_values), app -> startActivity(IdentraValuesActivity.forApp(ctx, app))));
        IdentraRows.row(tools, R.drawable.ic_identra_edit, getString(R.string.identra_tool_hook_editor), getString(R.string.identra_tool_hook_editor_desc),
                v -> startActivity(new Intent(ctx, IdentraHookEditorActivity.class)));

        IdentraRows.row(tools, R.drawable.ic_identra_layers, getString(R.string.identra_tool_instances), getString(R.string.identra_tool_instances_desc),
                v -> startActivity(new Intent(ctx, dev.identra.app.ui.tools.IdentraInstancesActivity.class)));

        LinearLayout backup = IdentraRows.card(list);
        IdentraRows.row(backup, R.drawable.ic_identra_backup, getString(R.string.identra_tool_backup), getString(R.string.identra_tool_backup_desc),
                v -> startActivity(new Intent(ctx, IdentraBackupActivity.class)));
        IdentraRows.row(backup, R.drawable.ic_identra_logs, getString(R.string.identra_tool_logs), getString(R.string.identra_tool_logs_desc2),
                v -> IdentraAppPicker.show(ctx, getString(R.string.identra_tool_logs), app -> startActivity(IdentraLogActivity.intent(ctx, app))));

        // ---- About ----
        IdentraRows.section(list, getString(R.string.identra_settings_about));
        LinearLayout about = IdentraRows.card(list);
        IdentraRows.row(about, R.drawable.ic_identra_help, getString(R.string.identra_about_help), getString(R.string.identra_help_row_desc),
                v -> startActivity(new Intent(ctx, IdentraHelpActivity.class)));
        IdentraRows.row(about, R.drawable.ic_identra_info, getString(R.string.app_name),
                getString(R.string.identra_about_version_code, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), null);
    }

    private void loadServiceValues() {
        final Context app = requireContext().getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            boolean notify = false, verbose = false, ok = true;
            try {
                notify = GetSettingExCommand.notifyOnNewApps(app);
                verbose = GetSettingExCommand.getVerboseLogs(app);
            } catch (Throwable t) {
                Log.e(TAG, "Read settings failed: " + t);
                ok = false;
            }
            final boolean fNotify = notify, fVerbose = verbose, fOk = ok;
            main.post(() -> {
                if (!isAdded() || notifySwitch == null) return;
                notifySwitch.setChecked(fNotify);
                verboseSwitch.setChecked(fVerbose);
                notifySwitch.setEnabled(fOk);
                verboseSwitch.setEnabled(fOk);
                notifySwitch.setOnCheckedChangeListener((b, on) ->
                        IdentraData.EXECUTOR.submit(() -> report(PutSettingExCommand.putNotifyNewApps(app, on))));
                verboseSwitch.setOnCheckedChangeListener((b, on) -> {
                    DebugUtil.setForceDebug(on);
                    IdentraData.EXECUTOR.submit(() -> report(PutSettingExCommand.putVerboseLogging(app, on)));
                });
            });
        });
    }

    private void report(final A_CODE code) {
        main.post(() -> {
            View v = getView();
            if (v != null && !A_CODE.isSuccessful(code))
                Snackbar.make(v, getString(R.string.identra_change_failed, String.valueOf(code)), Snackbar.LENGTH_LONG).show();
        });
    }

    private void applyNightMode() {
        // AppCompat recreates the activity itself when the effective mode changes
        AppCompatDelegate.setDefaultNightMode(IdentraTheme.nightMode(requireContext()));
        main.post(() -> { if (isAdded() && list != null) rebuild(); });
    }
}
