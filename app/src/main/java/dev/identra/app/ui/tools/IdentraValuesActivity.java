package dev.identra.app.ui.tools;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.identra.app.R;
import dev.identra.app.ui.IdentraData;
import dev.identra.app.ui.IdentraListAdapter;
import dev.identra.app.ui.IdentraScreenActivity;
import dev.identra.app.ui.IdentraSheet;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.call.PutSettingExCommand;
import dev.identra.app.x.core.commands.query.GetSettingsExCommand;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.ActionFlag;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.settings.data.SettingPacket;
import dev.identra.app.ui.profiles.ProfileGenerator;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

/**
 * Global values / per-app values (0.3.0, full editor since 0.5.0). Reads with GetSettingsExCommand
 * (every known setting key + saved values) and writes with PutSettingExCommand (PUSH / DELETE).
 * The edit sheet offers the module's randomizer for keys that have one (RandomizersCache, run
 * headless like the profile generator), clear/reset, and a custom key can be added.
 */
public class IdentraValuesActivity extends IdentraScreenActivity {
    private static final String TAG = "Identra.UiValues";
    private static final String EXTRA_PKG = "identra.values.pkg";
    private static final String EXTRA_UID = "identra.values.uid";
    private static final String EXTRA_LABEL = "identra.values.label";
    private static final int MENU_ADD = 1;
    private static final int CHIP_ALL = 1001, CHIP_SET = 1002, CHIP_UNSET = 1003;

    public static Intent global(Context c) { return new Intent(c, IdentraValuesActivity.class); }

    public static Intent forApp(Context c, AppXpPacket app) {
        Intent i = new Intent(c, IdentraValuesActivity.class);
        i.putExtra(EXTRA_PKG, app.packageName);
        i.putExtra(EXTRA_UID, app.uid);
        i.putExtra(EXTRA_LABEL, IdentraData.labelOf(app));
        return i;
    }

    private final IdentraListAdapter adapter = new IdentraListAdapter();
    private final List<SettingPacket> values = new ArrayList<>();
    private final Map<String, String> globals = new HashMap<>();
    private String pkg;
    private int uid;
    private String query = "";

    private boolean isGlobal() { return pkg == null; }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pkg = getIntent().getStringExtra(EXTRA_PKG);
        uid = getIntent().getIntExtra(EXTRA_UID, Process.myUid());
        if (isGlobal()) setScreenTitle(R.string.identra_tool_values);
        else setTitle(getIntent().getStringExtra(EXTRA_LABEL));

        useListBody(R.string.identra_values_search, q -> { query = q.toLowerCase(Locale.ROOT); render(); }).setAdapter(adapter);
        addChip(CHIP_ALL, R.string.identra_filter_all);
        addChip(CHIP_SET, R.string.identra_values_set);
        addChip(CHIP_UNSET, R.string.identra_values_unset);
        chips.setOnCheckedStateChangeListener((g, ids) -> render());

        toolbar.getMenu().add(Menu.NONE, MENU_ADD, Menu.NONE, R.string.identra_values_add)
                .setIcon(R.drawable.ic_identra_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_ADD) { addCustom(); return true; }
            return false;
        });
        load();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        load();
    }

    private void addCustom() {
        final IdentraSheet s = new IdentraSheet(this).title(getString(R.string.identra_values_add))
                .sub(getString(R.string.identra_values_add_desc));
        final TextInputEditText key = s.input(getString(R.string.identra_values_key), null, false);
        final TextInputEditText val = s.input(getString(R.string.identra_values_value), null, true);
        s.action(IdentraSheet.STYLE_FILLED, getString(R.string.identra_save), v -> {
            String k = key.getText() == null ? "" : key.getText().toString().trim();
            if (k.isEmpty()) return;
            SettingPacket p = null;
            for (SettingPacket e : values) if (e.name.equalsIgnoreCase(k)) p = e;
            if (p == null) {
                p = new SettingPacket();
                p.name = k;
                values.add(p);
                Collections.sort(values, (a, b) -> a.name.compareToIgnoreCase(b.name));
            }
            write(p, val.getText() == null ? "" : val.getText().toString(), false);
        }, true);
        s.show();
    }

    private static boolean hasRandomizer(String name) {
        try { return RandomizersCache.getCopy().containsKey(name); } catch (Throwable t) { return false; }
    }

    private String category() { return isGlobal() ? UserIdentity.GLOBAL_NAMESPACE : pkg; }

    private void load() {
        setBusy(true);
        final Context app = getApplicationContext();
        final String cat = category();
        final int u = isGlobal() ? Process.myUid() : uid;
        final boolean global = isGlobal();
        IdentraData.EXECUTOR.submit(() -> {
            List<SettingPacket> list = null;
            Map<String, String> g = null;
            try {
                list = GetSettingsExCommand.get(app, true, u, cat, GetSettingsExCommand.FLAG_ONE);
                if (!global) g = GetSettingsExCommand.getAsMap(app, true, Process.myUid(), UserIdentity.GLOBAL_NAMESPACE, GetSettingsExCommand.FLAG_ONE);
            } catch (Throwable t) {
                Log.e(TAG, "Load failed: " + t);
            }
            final List<SettingPacket> fl = list;
            final Map<String, String> fg = g;
            main.post(() -> {
                if (isFinishing()) return;
                setBusy(false);
                values.clear();
                if (fl != null) for (SettingPacket p : fl) if (p != null && !Str.isEmpty(p.name)) values.add(p);
                Collections.sort(values, (a, b) -> a.name.compareToIgnoreCase(b.name));
                globals.clear();
                if (fg != null) globals.putAll(fg);
                render();
            });
        });
    }

    private static String prefix(String name) {
        int i = name.indexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }

    private void render() {
        int chip = chips == null ? CHIP_ALL : chips.getCheckedChipId();
        List<IdentraListAdapter.Item> items = new ArrayList<>();
        String lastGroup = null;
        for (final SettingPacket p : values) {
            boolean set = p.value != null;
            if (chip == CHIP_SET && !set) continue;
            if (chip == CHIP_UNSET && set) continue;
            if (!query.isEmpty()) {
                boolean hit = p.name.toLowerCase(Locale.ROOT).contains(query)
                        || (p.description != null && p.description.toLowerCase(Locale.ROOT).contains(query))
                        || (p.value != null && p.value.toLowerCase(Locale.ROOT).contains(query));
                if (!hit) continue;
            }
            String group = prefix(p.name);
            if (!group.equalsIgnoreCase(lastGroup)) {
                items.add(IdentraListAdapter.Item.header(group.toUpperCase(Locale.ROOT)));
                lastGroup = group;
            }
            CharSequence sub;
            if (set) sub = p.value;
            else if (!isGlobal() && globals.get(p.name) != null) sub = getString(R.string.identra_values_inherits, globals.get(p.name));
            else sub = Str.isEmpty(p.description) ? getString(R.string.identra_values_not_set) : p.description;
            IdentraListAdapter.Item it = IdentraListAdapter.Item.row(0, p.name, sub)
                    .trailing(set ? getString(R.string.identra_values_set_badge) : null)
                    .click(v -> edit(p));
            it.chevron = false;
            items.add(it);
        }
        adapter.submit(items);
        boolean searching = !query.isEmpty() || chip != CHIP_ALL;
        showEmpty(items.isEmpty(), searching ? R.drawable.ill_identra_search : R.drawable.ill_identra_empty,
                searching ? R.string.identra_nothing_found : R.string.identra_values_empty, 0);
    }

    private void edit(final SettingPacket p) {
        final IdentraSheet s = new IdentraSheet(this).title(p.name).sub(p.description);
        if (!isGlobal() && globals.get(p.name) != null) s.field(getString(R.string.identra_values_global_value), globals.get(p.name));
        final TextInputEditText input = s.input(getString(R.string.identra_values_value), p.value, true);
        s.action(IdentraSheet.STYLE_FILLED, getString(R.string.identra_save), v -> {
            String value = input.getText() == null ? "" : input.getText().toString();
            write(p, value, false);
        }, true);
        if (hasRandomizer(p.name)) {
            final Context app = getApplicationContext();
            s.action(IdentraSheet.STYLE_TONAL, getString(R.string.identra_values_randomize), v ->
                    IdentraData.EXECUTOR.submit(() -> {
                        final String r = ProfileGenerator.existingRandomizer(app, p.name);
                        main.post(() -> {
                            if (r != null) input.setText(r);
                            else snack(getString(R.string.identra_values_randomize_failed));
                        });
                    }), false);
        }
        if (p.value != null)
            s.action(IdentraSheet.STYLE_TEXT, getString(isGlobal() ? R.string.identra_values_clear : R.string.identra_values_reset_app), v -> write(p, null, true), true);
        s.show();
    }

    private void write(final SettingPacket p, final String value, final boolean delete) {
        final Context app = getApplicationContext();
        final String name = p.name;
        final AppXpPacket target = isGlobal() ? null : IdentraData.findApp(IdentraData.cached(), pkg, uid);
        final boolean kill = target != null && target.forceStop;
        final UserIdentity id = UserIdentity.fromUid(isGlobal() ? Process.myUid() : uid, category());
        setBusy(true);
        IdentraData.EXECUTOR.submit(() -> {
            A_CODE code;
            try {
                SettingPacket packet = new SettingPacket(name, delete ? null : value,
                        ActionPacket.create(delete ? ActionFlag.DELETE : ActionFlag.PUSH, kill), id);
                code = PutSettingExCommand.call(app, packet);
            } catch (Throwable t) {
                Log.e(TAG, "Write failed: " + t);
                code = A_CODE.FAILED;
            }
            final A_CODE fc = code;
            main.post(() -> {
                if (isFinishing()) return;
                setBusy(false);
                if (A_CODE.isSuccessful(fc)) {
                    p.value = delete ? null : value;
                    snack(getString(delete ? R.string.identra_values_cleared : R.string.identra_values_saved, name));
                    render();
                } else {
                    snack(getString(R.string.identra_change_failed, String.valueOf(fc)));
                }
            });
        });
    }
}
