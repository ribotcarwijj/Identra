package dev.identra.app.ui.tools;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dev.identra.IdentraNs;
import dev.identra.app.R;
import dev.identra.app.ui.IdentraData;
import dev.identra.app.ui.IdentraListAdapter;
import dev.identra.app.ui.IdentraRows;
import dev.identra.app.ui.IdentraScreenActivity;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.query.GetHooksCommand;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.ui.adapters.hooks.elements.XHookIO;

/**
 * Hook editor (0.5.0). Without extras: the list of custom (user) hooks with a "New hook" action.
 * With {@link #EXTRA_HOOK}: a form to create, edit or duplicate a definition. Saved through
 * putHookExCommand into the Identra collection; built-in definitions are never overwritten by a
 * custom hook of the same name.
 */
public class IdentraHookEditorActivity extends IdentraScreenActivity {
    private static final String TAG = "Identra.UiHookEditor";
    private static final String EXTRA_HOOK = "identra.hookedit.json";
    private static final String EXTRA_MODE = "identra.hookedit.mode";
    private static final int MODE_NEW = 1, MODE_EDIT = 2, MODE_DUPLICATE = 3;
    private static final int MENU_NEW = 1;

    public static Intent newHook(Context c) {
        XHook h = XHook.create();
        h.collection = IdentraNs.COLLECTION;
        h.luaScript = "function after(hook, param)\n    -- param:getResult() / param:setResult(value)\n    return false\nend";
        h.enabled = true;
        return form(c, h, MODE_NEW);
    }

    public static Intent edit(Context c, XHook h) { return form(c, h, MODE_EDIT); }

    public static Intent duplicate(Context c, XHook from) {
        XHook h = XHook.copy(from);
        h.name = (from.name == null ? "hook" : from.name) + " (copy)";
        h.builtin = false;
        h.collection = IdentraNs.COLLECTION;
        return form(c, h, MODE_DUPLICATE);
    }

    private static Intent form(Context c, XHook h, int mode) {
        Intent i = new Intent(c, IdentraHookEditorActivity.class);
        i.putExtra(EXTRA_HOOK, XHookIO.toJsonString(h, 0));
        i.putExtra(EXTRA_MODE, mode);
        return i;
    }

    // ---- list mode ----
    private final IdentraListAdapter adapter = new IdentraListAdapter();
    private final List<XHook> custom = new ArrayList<>();
    private final Set<String> allNames = new HashSet<>();
    private final Set<String> builtinNames = new HashSet<>();

    // ---- form mode ----
    private XHook original;
    private int mode;
    private TextInputEditText fName, fGroup, fDesc, fClass, fMethod, fParams, fReturn, fSettings, fMinSdk, fMaxSdk, fTargets, fExcludes, fScript;
    private MaterialSwitch sEnabled, sOptional, sNotify;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String json = getIntent().getStringExtra(EXTRA_HOOK);
        if (json == null) {
            setScreenTitle(R.string.identra_tool_hook_editor);
            useListBody(0, null).setAdapter(adapter);
            toolbar.getMenu().add(Menu.NONE, MENU_NEW, Menu.NONE, R.string.identra_hookedit_new)
                    .setIcon(R.drawable.ic_identra_add)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == MENU_NEW) { startActivity(newHook(this)); return true; }
                return false;
            });
            loadList();
        } else {
            mode = getIntent().getIntExtra(EXTRA_MODE, MODE_NEW);
            original = XHook.create(json);
            setScreenTitle(mode == MODE_EDIT ? R.string.identra_hookedit_title_edit
                    : mode == MODE_DUPLICATE ? R.string.identra_hookedit_title_duplicate : R.string.identra_hookedit_title_new);
            buildForm();
            loadNames();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (original == null) loadList();
    }

    // ---------------------------------------------------------------- list

    private void loadList() {
        setBusy(true);
        final Context app = getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            List<XHook> all = null;
            try { all = GetHooksCommand.getHooks(app, true, true); } catch (Throwable t) { Log.e(TAG, "Load failed: " + t); }
            final List<XHook> fAll = all;
            main.post(() -> {
                if (isFinishing()) return;
                setBusy(false);
                custom.clear();
                if (fAll != null) for (XHook h : fAll) if (h != null && !h.isBuiltIn() && !Str.isEmpty(h.name)) custom.add(h);
                final Collator col = Collator.getInstance(Locale.getDefault());
                Collections.sort(custom, (a, b) -> col.compare(a.name, b.name));
                renderList();
            });
        });
    }

    private void renderList() {
        List<IdentraListAdapter.Item> items = new ArrayList<>();
        IdentraListAdapter.Item add = IdentraListAdapter.Item.row(R.drawable.ic_identra_add, getString(R.string.identra_hookedit_new),
                getString(R.string.identra_hookedit_new_desc)).click(v -> startActivity(newHook(this)));
        items.add(add);
        IdentraListAdapter.Item browse = IdentraListAdapter.Item.row(R.drawable.ic_identra_code, getString(R.string.identra_tool_hooks),
                getString(R.string.identra_hookedit_browse_desc)).click(v -> startActivity(IdentraHooksActivity.intent(this)));
        items.add(browse);
        if (!custom.isEmpty()) items.add(IdentraListAdapter.Item.header(getString(R.string.identra_hookedit_custom_header)));
        for (final XHook h : custom) {
            boolean disabled = h.enabled != null && !h.enabled;
            IdentraListAdapter.Item it = IdentraListAdapter.Item.row(R.drawable.ic_identra_edit, h.name,
                    IdentraData.groupTitle(this, h.group) + " · " + Str.getNonNullString(h.getClassName(), "") + "." + Str.getNonNullString(h.methodName, ""))
                    .trailing(disabled ? getString(R.string.identra_hooks_disabled) : null)
                    .click(v -> HookSheets.details(this, h, this::loadList));
            items.add(it);
        }
        adapter.submit(items);
        showEmpty(false, 0, R.string.identra_hookedit_none, 0);
    }

    // ---------------------------------------------------------------- form

    private void loadNames() {
        final Context app = getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            List<XHook> all = null;
            try { all = GetHooksCommand.getHooks(app, true, true); } catch (Throwable t) { Log.e(TAG, "Names failed: " + t); }
            final List<XHook> fAll = all;
            main.post(() -> {
                if (fAll == null) return;
                for (XHook h : fAll) {
                    if (h == null || h.name == null) continue;
                    allNames.add(h.name.toLowerCase(Locale.ROOT));
                    if (h.isBuiltIn()) builtinNames.add(h.name.toLowerCase(Locale.ROOT));
                }
            });
        });
    }

    private LinearLayout formCard(LinearLayout root, int title) {
        IdentraRows.section(root, getString(title));
        LinearLayout rows = IdentraRows.card(root);
        int p = IdentraRows.dp(this, 16);
        rows.setPadding(p, IdentraRows.dp(this, 4), p, IdentraRows.dp(this, 12));
        return rows;
    }

    private TextInputEditText field(LinearLayout parent, int hint, String value, boolean multi, int helper) {
        TextInputLayout til = (TextInputLayout) LayoutInflater.from(this).inflate(R.layout.item_identra_text_field, parent, false);
        til.setHint(getString(hint));
        if (helper != 0) til.setHelperText(getString(helper));
        TextInputEditText et = til.findViewById(R.id.identraFieldEdit);
        if (multi) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            et.setMinLines(2);
            et.setMaxLines(10);
            et.setGravity(Gravity.TOP | Gravity.START);
        } else {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            et.setMaxLines(1);
        }
        if (value != null) et.setText(value);
        parent.addView(til);
        return et;
    }

    private static String lines(List<String> l) { return l == null || l.isEmpty() ? "" : Str.joinList(l, "\n"); }

    private void buildForm() {
        LinearLayout root = useScrollBody();
        XHook h = original;

        TextView intro = new TextView(this);
        intro.setTextAppearance(R.style.TextAppearance_Identra_RowSub);
        intro.setText(mode == MODE_EDIT ? R.string.identra_hookedit_intro_edit : R.string.identra_hookedit_intro_new);
        intro.setPadding(IdentraRows.dp(this, 4), IdentraRows.dp(this, 4), IdentraRows.dp(this, 4), IdentraRows.dp(this, 8));
        root.addView(intro);

        LinearLayout basics = formCard(root, R.string.identra_hookedit_sec_basics);
        fName = field(basics, R.string.identra_hookedit_name, h.name, false, R.string.identra_hookedit_name_help);
        fGroup = field(basics, R.string.identra_hook_group, h.group, false, R.string.identra_hookedit_group_help);
        fDesc = field(basics, R.string.identra_hook_description, h.description, true, 0);

        LinearLayout target = formCard(root, R.string.identra_hookedit_sec_target);
        fClass = field(target, R.string.identra_hook_class, h.className, false, R.string.identra_hookedit_class_help);
        fMethod = field(target, R.string.identra_hook_method, h.methodName, false, R.string.identra_hookedit_method_help);
        fParams = field(target, R.string.identra_hook_params, lines(h.parameterTypes), true, R.string.identra_hookedit_lines_help);
        fReturn = field(target, R.string.identra_hook_return, h.returnType, false, 0);
        fMinSdk = field(target, R.string.identra_hookedit_min_sdk, String.valueOf(h.minSdk), false, 0);
        fMinSdk.setInputType(InputType.TYPE_CLASS_NUMBER);
        fMaxSdk = field(target, R.string.identra_hookedit_max_sdk, String.valueOf(h.maxSdk), false, 0);
        fMaxSdk.setInputType(InputType.TYPE_CLASS_NUMBER);

        LinearLayout values = formCard(root, R.string.identra_hookedit_sec_values);
        fSettings = field(values, R.string.identra_hook_settings, lines(h.settings), true, R.string.identra_hookedit_lines_help);
        fTargets = field(values, R.string.identra_hook_targets, lines(h.targetPackages), true, R.string.identra_hookedit_pkgs_help);
        fExcludes = field(values, R.string.identra_hook_excludes, lines(h.excludePackages), true, R.string.identra_hookedit_pkgs_help);

        LinearLayout script = formCard(root, R.string.identra_hook_script);
        fScript = field(script, R.string.identra_hookedit_script_hint, h.luaScript, true, R.string.identra_hookedit_script_help);
        fScript.setTypeface(Typeface.MONOSPACE);
        fScript.setMinLines(8);
        fScript.setMaxLines(Integer.MAX_VALUE);
        fScript.setHorizontallyScrolling(false);

        IdentraRows.section(root, getString(R.string.identra_hook_flags));
        LinearLayout flags = IdentraRows.card(root);
        sEnabled = IdentraRows.switchRow(flags, R.drawable.ic_identra_check, getString(R.string.identra_hookedit_enabled),
                getString(R.string.identra_hookedit_enabled_desc), h.enabled == null || h.enabled, null);
        sOptional = IdentraRows.switchRow(flags, R.drawable.ic_identra_shield, getString(R.string.identra_hook_optional),
                getString(R.string.identra_hookedit_optional_desc), Boolean.TRUE.equals(h.optional), null);
        sNotify = IdentraRows.switchRow(flags, R.drawable.ic_identra_bell, getString(R.string.identra_hook_notify),
                getString(R.string.identra_hookedit_notify_desc), Boolean.TRUE.equals(h.notify), null);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.VERTICAL);
        buttons.setPadding(0, IdentraRows.dp(this, 12), 0, 0);
        MaterialButton save = (MaterialButton) LayoutInflater.from(this).inflate(R.layout.item_identra_button_filled, buttons, false);
        save.setText(R.string.identra_save);
        save.setOnClickListener(v -> save());
        buttons.addView(save);
        MaterialButton cancel = (MaterialButton) LayoutInflater.from(this).inflate(R.layout.item_identra_button_text, buttons, false);
        cancel.setText(android.R.string.cancel);
        cancel.setOnClickListener(v -> finish());
        buttons.addView(cancel);
        root.addView(buttons);
    }

    private static String text(TextInputEditText et) {
        return et == null || et.getText() == null ? "" : et.getText().toString().trim();
    }

    private static List<String> splitLines(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        for (String part : s.split("[\\n,]")) {
            String t = part.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static int intOr(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Throwable t) { return def; }
    }

    private boolean require(TextInputEditText et, String value) {
        if (!value.isEmpty()) return true;
        View p = (View) et.getParent().getParent();
        if (p instanceof TextInputLayout) ((TextInputLayout) p).setError(getString(R.string.identra_hookedit_required));
        et.requestFocus();
        return false;
    }

    private void save() {
        String name = text(fName), group = text(fGroup), cls = text(fClass), method = text(fMethod);
        if (!require(fName, name) || !require(fGroup, group) || !require(fClass, cls) || !require(fMethod, method)) return;
        String low = name.toLowerCase(Locale.ROOT);
        boolean renamed = mode != MODE_EDIT || original.name == null || !original.name.equalsIgnoreCase(name);
        if (builtinNames.contains(low) || (renamed && allNames.contains(low))) {
            View p = (View) fName.getParent().getParent();
            if (p instanceof TextInputLayout) ((TextInputLayout) p).setError(getString(R.string.identra_hookedit_name_taken));
            fName.requestFocus();
            return;
        }
        final XHook h = XHook.copy(original);
        h.collection = IdentraNs.COLLECTION;
        h.builtin = false;
        h.name = name;
        h.group = group;
        h.description = text(fDesc);
        h.className = cls;
        h.resolvedClassName = null;
        h.methodName = method;
        h.parameterTypes.clear();
        h.parameterTypes.addAll(splitLines(text(fParams)));
        String ret = text(fReturn);
        h.returnType = ret.isEmpty() ? null : ret;
        h.minSdk = intOr(text(fMinSdk), 0);
        h.maxSdk = intOr(text(fMaxSdk), 999);
        h.settings.clear();
        h.settings.addAll(splitLines(text(fSettings)));
        h.targetPackages.clear();
        h.targetPackages.addAll(splitLines(text(fTargets)));
        h.excludePackages.clear();
        h.excludePackages.addAll(splitLines(text(fExcludes)));
        String script = fScript.getText() == null ? "" : fScript.getText().toString();
        h.luaScript = script.trim().isEmpty() ? XHook.DEFAULT_SCRIPT : script;
        h.enabled = sEnabled.isChecked();
        h.optional = sOptional.isChecked();
        h.notify = sNotify.isChecked();
        h.usage = original.usage == null ? Boolean.TRUE : original.usage;
        h.author = Str.isEmpty(original.author) || original.isBuiltIn() ? "Identra" : original.author;
        h.version = original.version == null ? 1 : original.version + (mode == MODE_EDIT ? 1 : 0);
        h.internalId = null;

        final XHook old = mode == MODE_EDIT && renamed ? XHook.copy(original) : null;
        setBusy(true);
        HookSheets.save(this, h, false, () -> {
            if (old != null) HookSheets.save(this, old, true, null);   // a rename replaces the old definition
            finish();
        });
        main.postDelayed(() -> setBusy(false), 1500);
    }
}
