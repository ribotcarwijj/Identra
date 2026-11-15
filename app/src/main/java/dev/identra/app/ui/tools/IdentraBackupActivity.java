package dev.identra.app.ui.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import dev.identra.app.R;
import dev.identra.app.ui.IdentraData;
import dev.identra.app.ui.IdentraScreenActivity;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.settings.data.XBackup;
import dev.identra.app.x.ui.FileDialogUtils;
import dev.identra.app.x.ui.dialogs.BackupDialog;
import dev.identra.app.x.ui.dialogs.utils.BackupDialogUtils;

/**
 * Backup & Restore, rebuilt (0.3.0). Same flow and file format as the classic screen:
 * BackupDialog picks what to include, BackupDialogUtils opens the system file pickers, and on
 * restore BackupDialog itself applies the selected parts (with its progress dialog).
 */
public class IdentraBackupActivity extends IdentraScreenActivity {
    private static final String TAG = "Identra.UiBackup";
    private final XBackup backup = new XBackup();
    private LinearLayout content;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreenTitle(R.string.identra_tool_backup);
        content = useScrollBody();
        card(R.drawable.ic_identra_backup, R.string.identra_backup_title, R.string.identra_backup_body,
                R.string.identra_backup_action, R.layout.item_identra_button_filled, v -> exportBackup());
        card(R.drawable.ic_identra_restore, R.string.identra_restore_title, R.string.identra_restore_body,
                R.string.identra_restore_action, R.layout.item_identra_button_tonal, v -> BackupDialogUtils.startBackupFilePicker(this));
        TextView note = new TextView(this);
        note.setTextAppearance(R.style.TextAppearance_Identra_Caption);
        note.setText(R.string.identra_backup_note);
        int p = dev.identra.app.ui.IdentraRows.dp(this, 4);
        note.setPadding(p, p * 3, p, 0);
        content.addView(note);
    }

    private void card(int icon, int title, int body, int action, int buttonLayout, View.OnClickListener onClick) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_identra_action_card, content, false);
        ((ImageView) card.findViewById(R.id.actionCardIcon)).setImageResource(icon);
        ((TextView) card.findViewById(R.id.actionCardTitle)).setText(title);
        ((TextView) card.findViewById(R.id.actionCardBody)).setText(body);
        LinearLayout box = card.findViewById(R.id.actionCardButtons);
        MaterialButton b = (MaterialButton) LayoutInflater.from(this).inflate(buttonLayout, box, false);
        b.setText(action);
        b.setOnClickListener(onClick);
        box.addView(b);
        content.addView(card);
    }

    private void exportBackup() {
        setBusy(true);
        final Context ctx = this;
        backup.reset();
        IdentraData.EXECUTOR.submit(() -> {
            BackupDialog dialog = null;
            try {
                dialog = BackupDialog.create()
                        .setBackup(backup, false)
                        .setDefinitions(ctx)
                        .setSettings(ctx)
                        .setAssignments(ctx)
                        .setAllHooks(ctx);
            } catch (Throwable t) {
                Log.e(TAG, "Backup prepare failed: " + t);
            }
            final BackupDialog fd = dialog;
            main.post(() -> {
                if (isFinishing()) return;
                setBusy(false);
                if (fd == null) { snack(getString(R.string.msg_backup_exported_failed)); return; }
                fd.setOnOperationExportedOrApplied((v, wasImported) -> {
                    if (v != null && !wasImported) {
                        backup.copyFrom(v);
                        BackupDialogUtils.startSavePicker(IdentraBackupActivity.this, backup.getName());
                    }
                }).show(getSupportFragmentManager(), getString(R.string.title_backup));
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null || resultCode != Activity.RESULT_OK) return;
        final Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == BackupDialogUtils.REQUEST_OPEN_BACKUP) {
            XBackup fromFile = BackupDialogUtils.readBackupFromUri(this, uri);
            if (fromFile == null) {
                snack(getString(R.string.msg_error_reading_backup));
                return;
            }
            // BackupDialog applies the chosen parts itself (settings, definitions, assignments).
            BackupDialog.create()
                    .setBackup(fromFile, true)
                    .setOnOperationExportedOrApplied((v, wasImported) -> {
                        if (wasImported) {
                            snack(getString(R.string.msg_settings_imported));
                            IdentraData.refresh(this, s -> { });
                        }
                    })
                    .show(getSupportFragmentManager(), getString(R.string.title_backup));
        } else if (requestCode == BackupDialogUtils.REQUEST_SAVE_BACKUP) {
            try {
                String json = backup.toJSON();
                if (Str.isEmpty(json)) throw new Exception("Identra backup JSON is empty");
                FileDialogUtils.takePersistablePermissions(this, uri);
                boolean ok = FileDialogUtils.writeFileContent(this, uri, json);
                snack(getString(ok ? R.string.msg_backup_exported : R.string.msg_backup_exported_failed));
            } catch (Exception e) {
                Log.e(TAG, "Saving backup failed: " + e);
                snack(getString(R.string.msg_backup_exported_failed));
            }
        }
    }
}
