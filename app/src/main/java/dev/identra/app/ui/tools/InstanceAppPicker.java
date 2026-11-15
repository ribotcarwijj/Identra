package dev.identra.app.ui.tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dev.identra.app.R;
import dev.identra.app.ui.IdentraRows;

/**
 * 0.7.34 app picker for instances: search, icons, user + system apps, selected apps on top.
 * Keyboards (input methods) are pre-ticked for a new instance (typing needs one) but can be unticked.
 */
final class InstanceAppPicker {
    interface Done { void onPicked(List<String> pkgs); }

    private static final class Item {
        final String pkg, label; final boolean system, keyboard; Drawable icon; boolean checked;
        Item(String pkg, String label, boolean system, boolean keyboard) { this.pkg = pkg; this.label = label; this.system = system; this.keyboard = keyboard; }
    }

    private static Set<String> imeCache;

    /** Packages that provide a keyboard (InputMethodService) on the owner profile. */
    static synchronized Set<String> keyboards(Context c) {
        if (imeCache != null) return imeCache;
        Set<String> s = new HashSet<>();
        try {
            InputMethodManager imm = c.getSystemService(InputMethodManager.class);
            for (InputMethodInfo i : imm.getInputMethodList()) s.add(i.getPackageName());
        } catch (Throwable ignored) { }
        if (s.isEmpty()) s.add("com.google.android.inputmethod.latin");
        imeCache = s;
        return s;
    }

    static boolean isKeyboard(Context c, String pkg) { return keyboards(c).contains(pkg); }

    /** IME ids (pkg/service) for the keyboards in {@code pkgs}, used to enable them inside the clone user. */
    static List<String> imeIds(Context c, List<String> pkgs) {
        List<String> out = new ArrayList<>();
        try {
            InputMethodManager imm = c.getSystemService(InputMethodManager.class);
            for (InputMethodInfo i : imm.getInputMethodList()) if (pkgs.contains(i.getPackageName())) out.add(i.getId());
        } catch (Throwable ignored) { }
        return out;
    }

    /** The keyboard the phone uses now (package), or Gboard. */
    static String defaultKeyboard(Context c) {
        try {
            String id = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            if (id != null && id.contains("/")) return id.substring(0, id.indexOf('/'));
        } catch (Throwable ignored) { }
        return "com.google.android.inputmethod.latin";
    }

    static void show(Activity a, String title, List<String> preselected, boolean isNew, Done cb) {
        PackageManager pm = a.getPackageManager();
        Set<String> kb = keyboards(a);
        Set<String> launchable = new HashSet<>();
        for (ResolveInfo ri : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0))
            launchable.add(ri.activityInfo.packageName);
        Set<String> pre = new LinkedHashSet<>(preselected);
        if (isNew) { String d = defaultKeyboard(a); if (kb.contains(d)) pre.add(d); else pre.addAll(kb.contains("com.google.android.inputmethod.latin") ? Collections.singleton("com.google.android.inputmethod.latin") : Collections.emptySet()); }

        final List<Item> all = new ArrayList<>();
        for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
            String p = ai.packageName;
            if (a.getPackageName().equals(p) || IdentraInstancesActivity.isGmsPkg(p)) continue;
            boolean sys = (ai.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            boolean isKb = kb.contains(p);
            // system apps: only ones a person would recognise (launchable or a keyboard), unless already chosen
            if (sys && !launchable.contains(p) && !isKb && !pre.contains(p)) continue;
            Item it = new Item(p, String.valueOf(ai.loadLabel(pm)), sys, isKb);
            it.checked = pre.contains(p);
            all.add(it);
        }
        Collections.sort(all, (x, y) -> {
            if (x.checked != y.checked) return x.checked ? -1 : 1;
            if (x.system != y.system) return x.system ? 1 : -1;
            return x.label.compareToIgnoreCase(y.label);
        });

        final boolean[] showSystem = { false };
        final List<Item> shown = new ArrayList<>();
        final String[] query = { "" };
        final Map<String, Drawable> icons = new HashMap<>();
        final int dp8 = IdentraRows.dp(a, 8), dp16 = IdentraRows.dp(a, 16), icon = IdentraRows.dp(a, 40);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp16, dp8, dp16, 0);
        EditText search = new EditText(a);
        search.setHint(R.string.identra_picker_search);
        search.setSingleLine(true);
        root.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        CheckBox sysBox = new CheckBox(a);
        sysBox.setText(R.string.identra_picker_show_system);
        root.addView(sysBox);
        TextView count = new TextView(a);
        count.setTextAppearance(R.style.TextAppearance_Identra_Caption);
        count.setPadding(0, 0, 0, dp8);
        root.addView(count);
        ListView list = new ListView(a);
        list.setDividerHeight(0);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, IdentraRows.dp(a, 420)));

        final BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return shown.size(); }
            @Override public Object getItem(int i) { return shown.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int pos, View v, ViewGroup parent) {
                LinearLayout row;
                if (v == null) {
                    row = new LinearLayout(a);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(0, dp8, 0, dp8);
                    ImageView iv = new ImageView(a);
                    row.addView(iv, new LinearLayout.LayoutParams(icon, icon));
                    LinearLayout texts = new LinearLayout(a);
                    texts.setOrientation(LinearLayout.VERTICAL);
                    texts.setPadding(dp16, 0, dp8, 0);
                    TextView t1 = new TextView(a); t1.setTextAppearance(R.style.TextAppearance_Identra_CardTitle);
                    TextView t2 = new TextView(a); t2.setTextAppearance(R.style.TextAppearance_Identra_Caption);
                    t2.setSingleLine(true); t2.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
                    texts.addView(t1); texts.addView(t2);
                    row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    CheckBox cb = new CheckBox(a); cb.setClickable(false); cb.setFocusable(false);
                    row.addView(cb);
                } else row = (LinearLayout) v;
                Item it = shown.get(pos);
                ImageView iv = (ImageView) row.getChildAt(0);
                Drawable d = icons.get(it.pkg);
                if (d == null) { try { d = pm.getApplicationIcon(it.pkg); } catch (Throwable t) { d = pm.getDefaultActivityIcon(); } icons.put(it.pkg, d); }
                iv.setImageDrawable(d);
                LinearLayout texts = (LinearLayout) row.getChildAt(1);
                ((TextView) texts.getChildAt(0)).setText(it.label);
                String tag = it.keyboard ? a.getString(R.string.identra_picker_keyboard) : it.system ? a.getString(R.string.identra_picker_system) : null;
                ((TextView) texts.getChildAt(1)).setText(tag == null ? it.pkg : tag + " · " + it.pkg);
                ((CheckBox) row.getChildAt(2)).setChecked(it.checked);
                return row;
            }
        };
        list.setAdapter(adapter);

        final Runnable filter = () -> {
            shown.clear();
            String q = query[0].toLowerCase(Locale.ROOT);
            int sel = 0;
            for (Item it : all) {
                if (it.checked) sel++;
                boolean match = q.isEmpty() || it.label.toLowerCase(Locale.ROOT).contains(q) || it.pkg.contains(q);
                // checked and keyboard rows always visible; other system apps only with the switch or a search
                boolean vis = it.checked || it.keyboard || !it.system || showSystem[0] || !q.isEmpty();
                if (match && vis) shown.add(it);
            }
            count.setText(a.getResources().getQuantityString(R.plurals.identra_picker_selected, sel, sel));
            adapter.notifyDataSetChanged();
        };
        list.setOnItemClickListener((p, v, pos, id) -> { Item it = shown.get(pos); it.checked = !it.checked; filter.run(); });
        sysBox.setOnCheckedChangeListener((b, on) -> { showSystem[0] = on; filter.run(); });
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            public void onTextChanged(CharSequence s, int st, int b, int c) { query[0] = s.toString().trim(); filter.run(); }
            public void afterTextChanged(Editable e) { }
        });
        filter.run();

        new AlertDialog.Builder(a)
                .setTitle(title)
                .setView(root)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    List<String> sel = new ArrayList<>();
                    boolean realApp = false;
                    for (Item it : all) if (it.checked) { sel.add(it.pkg); if (!it.keyboard) realApp = true; }
                    if (!realApp) Toast.makeText(a, R.string.identra_instances_pick_one, Toast.LENGTH_SHORT).show();
                    else cb.onPicked(sel);
                }).show();
    }
}
