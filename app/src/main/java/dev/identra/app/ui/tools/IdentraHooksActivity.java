package dev.identra.app.ui.tools;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import dev.identra.app.R;
import dev.identra.app.ui.IdentraData;
import dev.identra.app.ui.IdentraListAdapter;
import dev.identra.app.ui.IdentraScreenActivity;
import dev.identra.app.ui.ProtectionCategories;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.query.GetHooksCommand;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;

/**
 * Hooks browser (0.5.0): every hook of the Identra collection, grouped category → group,
 * searchable. Tapping a hook opens {@link HookSheets#details}: definition, values it reads, the
 * Lua script, turn on/off for an app, enable/disable everywhere, duplicate, and edit/delete for
 * custom hooks. New hooks are created in {@link IdentraHookEditorActivity}.
 */
public class IdentraHooksActivity extends IdentraScreenActivity {
    private static final String TAG = "Identra.UiHooks";
    private static final int MENU_NEW = 1;

    private final IdentraListAdapter adapter = new IdentraListAdapter();
    private final List<XHook> hooks = new ArrayList<>();
    private final Set<String> expanded = new HashSet<>();
    private String query = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreenTitle(R.string.identra_tool_hooks);
        useListBody(R.string.identra_hooks_search, q -> { query = q.toLowerCase(Locale.ROOT); render(); }).setAdapter(adapter);
        toolbar.getMenu().add(Menu.NONE, MENU_NEW, Menu.NONE, R.string.identra_hookedit_new)
                .setIcon(R.drawable.ic_identra_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_NEW) { startActivity(IdentraHookEditorActivity.newHook(this)); return true; }
            return false;
        });
        load();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        load();   // the editor may have changed definitions
    }

    void load() {
        setBusy(true);
        final Context app = getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            List<XHook> all = null;
            try {
                all = GetHooksCommand.getHooks(app, true, true);
            } catch (Throwable t) {
                Log.e(TAG, "Load failed: " + t);
            }
            final List<XHook> fAll = all;
            main.post(() -> {
                if (isFinishing()) return;
                setBusy(false);
                hooks.clear();
                if (fAll != null) for (XHook h : fAll) if (h != null && !Str.isEmpty(h.getObjectId())) hooks.add(h);
                render();
            });
        });
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private boolean matches(XHook h) {
        if (query.isEmpty()) return true;
        return nz(h.name).toLowerCase(Locale.ROOT).contains(query)
                || nz(h.group).toLowerCase(Locale.ROOT).contains(query)
                || nz(h.getClassName()).toLowerCase(Locale.ROOT).contains(query)
                || nz(h.methodName).toLowerCase(Locale.ROOT).contains(query)
                || nz(h.description).toLowerCase(Locale.ROOT).contains(query)
                || Str.joinList(h.settings, " ").toLowerCase(Locale.ROOT).contains(query);
    }

    private void render() {
        final Collator col = Collator.getInstance(Locale.getDefault());
        // category -> group -> hooks
        Map<String, Map<String, List<XHook>>> tree = new LinkedHashMap<>();
        for (ProtectionCategories.Category c : ProtectionCategories.ALL) tree.put(c.key, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        tree.put(ProtectionCategories.OTHER, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        for (XHook h : hooks) {
            if (!matches(h)) continue;
            String g = Str.isEmpty(h.group) ? getString(R.string.identra_hooks_no_group) : h.group;
            Map<String, List<XHook>> groups = tree.get(ProtectionCategories.categoryOf(h.group));
            if (groups == null) groups = tree.get(ProtectionCategories.OTHER);
            List<XHook> l = groups.get(g);
            if (l == null) { l = new ArrayList<>(); groups.put(g, l); }
            l.add(h);
        }
        List<IdentraListAdapter.Item> items = new ArrayList<>();
        boolean searching = !query.isEmpty();
        for (Map.Entry<String, Map<String, List<XHook>>> c : tree.entrySet()) {
            if (c.getValue().isEmpty()) continue;
            items.add(IdentraListAdapter.Item.header(getString(ProtectionCategories.byKey(c.getKey()).title)));
            for (Map.Entry<String, List<XHook>> g : c.getValue().entrySet()) {
                final String key = g.getKey();
                final boolean open = searching || expanded.contains(key);
                List<XHook> list = g.getValue();
                Collections.sort(list, (a, b) -> col.compare(nz(a.name), nz(b.name)));
                String title = IdentraData.groupTitle(this, key);
                IdentraListAdapter.Item gi = IdentraListAdapter.Item.row(R.drawable.ic_identra_code, title,
                        getResources().getQuantityString(R.plurals.identra_hooks_count, list.size(), list.size())
                                + (key.equals(title) ? "" : " · " + key))
                        .click(v -> {
                            if (expanded.contains(key)) expanded.remove(key);
                            else expanded.add(key);
                            render();
                        });
                gi.rotateChevron = open;
                items.add(gi);
                if (!open) continue;
                for (final XHook h : list) {
                    boolean disabled = h.enabled != null && !h.enabled;
                    String sub = nz(h.getClassName()) + (Str.isEmpty(h.methodName) ? "" : "." + h.methodName);
                    IdentraListAdapter.Item hi = IdentraListAdapter.Item.row(0, nz(h.name), sub)
                            .trailing(disabled ? getString(R.string.identra_hooks_disabled) : (h.isBuiltIn() ? null : getString(R.string.identra_hooks_custom)))
                            .click(v -> HookSheets.details(this, h, this::load));
                    hi.chevron = false;
                    hi.indentDp = 52;
                    items.add(hi);
                }
            }
        }
        adapter.submit(items);
        showEmpty(items.isEmpty(), searching ? R.drawable.ill_identra_search : R.drawable.ill_identra_empty,
                searching ? R.string.identra_nothing_found : R.string.identra_hooks_empty, 0);
    }

    static Intent intent(Context c) { return new Intent(c, IdentraHooksActivity.class); }
}
