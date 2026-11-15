package dev.identra.app.ui.tools;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import dev.identra.app.R;
import dev.identra.app.ui.IdentraData;
import dev.identra.app.ui.IdentraListAdapter;
import dev.identra.app.ui.IdentraScreenActivity;
import dev.identra.app.ui.IdentraSheet;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.query.GetAssignmentsCommand;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.core.hook.AssignmentPacket;

/**
 * Per-app activity log, rebuilt (0.3.0). Same data as the classic LogDialog:
 * GetAssignmentsCommand.get(..., 3) gives each assigned hook with last use, old/new value
 * and exception.
 */
public class IdentraLogActivity extends IdentraScreenActivity {
    private static final String TAG = "Identra.UiLogs";
    private static final String EXTRA_PKG = "identra.log.pkg";
    private static final String EXTRA_UID = "identra.log.uid";
    private static final String EXTRA_LABEL = "identra.log.label";
    private static final String SPECIAL_PREFIX = "Setting:"; // same as LogDialog
    private static final int CHIP_USED = 2001, CHIP_ALL = 2002, CHIP_ERRORS = 2003;

    public static Intent intent(Context c, AppXpPacket app) {
        Intent i = new Intent(c, IdentraLogActivity.class);
        i.putExtra(EXTRA_PKG, app.packageName);
        i.putExtra(EXTRA_UID, app.uid);
        i.putExtra(EXTRA_LABEL, IdentraData.labelOf(app));
        return i;
    }

    private final IdentraListAdapter adapter = new IdentraListAdapter();
    private final List<AssignmentPacket> entries = new ArrayList<>();
    private String pkg;
    private int uid;
    private String query = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pkg = getIntent().getStringExtra(EXTRA_PKG);
        uid = getIntent().getIntExtra(EXTRA_UID, -1);
        setTitle(getString(R.string.identra_logs_title, getIntent().getStringExtra(EXTRA_LABEL)));
        useListBody(R.string.identra_logs_search, q -> { query = q.toLowerCase(Locale.ROOT); render(); }).setAdapter(adapter);
        addChip(CHIP_USED, R.string.identra_logs_used);
        addChip(CHIP_ALL, R.string.identra_filter_all);
        addChip(CHIP_ERRORS, R.string.identra_logs_errors);
        chips.setOnCheckedStateChangeListener((g, ids) -> render());
        load();
    }

    private void load() {
        setBusy(true);
        final Context app = getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            List<AssignmentPacket> got = null;
            try { got = GetAssignmentsCommand.get(app, true, uid, pkg, 3); } catch (Throwable t) { Log.e(TAG, "Load failed: " + t); }
            final List<AssignmentPacket> fGot = got;
            main.post(() -> {
                if (isFinishing()) return;
                setBusy(false);
                entries.clear();
                if (fGot != null) for (AssignmentPacket p : fGot) if (p != null && !Str.isEmpty(p.getHookId())) entries.add(p);
                Collections.sort(entries, (a, b) -> Long.compare(b.used, a.used));
                render();
            });
        });
    }

    private static String hookName(AssignmentPacket p) {
        String id = p.getHookId();
        if (id != null && id.startsWith(SPECIAL_PREFIX) && id.length() > SPECIAL_PREFIX.length()) return id.substring(SPECIAL_PREFIX.length());
        return id;
    }

    private static boolean hasValues(AssignmentPacket p) { return !Str.isEmpty(p.oldValue) || !Str.isEmpty(p.newValue); }

    private void render() {
        int chip = chips.getCheckedChipId();
        List<IdentraListAdapter.Item> items = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (final AssignmentPacket p : entries) {
            boolean used = p.used > 0 || hasValues(p);
            boolean error = !Str.isEmpty(p.exception);
            if (chip == CHIP_USED && !used) continue;
            if (chip == CHIP_ERRORS && !error) continue;
            String name = hookName(p);
            if (!query.isEmpty()) {
                String hay = (name + " " + Str.toStringOrNull(p.newValue) + " " + Str.toStringOrNull(p.oldValue)).toLowerCase(Locale.ROOT);
                if (!hay.contains(query)) continue;
            }
            String sub;
            if (error) sub = getString(R.string.identra_logs_error_short, firstLine(p.exception));
            else if (hasValues(p)) sub = getString(R.string.identra_logs_change, firstLine(p.oldValue), firstLine(p.newValue));
            else sub = getString(R.string.identra_logs_installed);
            String when = p.used > 0 ? DateUtils.getRelativeTimeSpanString(p.used, now, DateUtils.MINUTE_IN_MILLIS).toString() : null;
            IdentraListAdapter.Item it = IdentraListAdapter.Item.row(error ? R.drawable.ic_identra_info : R.drawable.ic_identra_code, name, sub)
                    .trailing(when)
                    .click(v -> show(p));
            it.chevron = false;
            items.add(it);
        }
        adapter.submit(items);
        boolean filtered = !query.isEmpty() || chip != CHIP_ALL;
        showEmpty(items.isEmpty(), filtered ? R.drawable.ill_identra_search : R.drawable.ill_identra_empty,
                filtered ? R.string.identra_logs_empty_filtered : R.string.identra_logs_empty, R.string.identra_logs_empty_body);
    }

    private static String firstLine(String s) {
        if (s == null) return "–";
        String t = s.trim();
        int nl = t.indexOf('\n');
        if (nl > 0) t = t.substring(0, nl);
        return t.length() > 60 ? t.substring(0, 60) + "…" : t;
    }

    private void show(AssignmentPacket p) {
        IdentraSheet s = new IdentraSheet(this).title(hookName(p)).sub(p.getHookId());
        if (p.installed > 0) s.field(getString(R.string.identra_logs_installed_at), DateUtils.formatDateTime(this, p.installed,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR));
        if (p.used > 0) s.field(getString(R.string.identra_logs_used_at), DateUtils.formatDateTime(this, p.used,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR));
        s.field(getString(R.string.identra_logs_restricted), getString(p.restricted ? R.string.identra_yes : R.string.identra_no));
        s.mono(getString(R.string.identra_logs_new), p.newValue);
        s.mono(getString(R.string.identra_logs_old), p.oldValue);
        s.mono(getString(R.string.identra_logs_exception), p.exception);
        s.action(IdentraSheet.STYLE_TONAL, getString(R.string.identra_copy), v -> IdentraSheet.copy(this, hookName(p),
                "new: " + Str.toStringOrNull(p.newValue) + "\nold: " + Str.toStringOrNull(p.oldValue)
                        + (Str.isEmpty(p.exception) ? "" : "\nexception: " + p.exception)), true);
        s.show();
    }
}
