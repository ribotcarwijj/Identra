package dev.identra.app.ui.profiles;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.identra.app.R;
import dev.identra.app.ui.AppDetailActivity;
import dev.identra.app.ui.IdentraData;
import dev.identra.app.ui.IdentraLocation;
import dev.identra.app.ui.IdentraPlaces;
import dev.identra.app.ui.IdentraRows;
import dev.identra.app.ui.IdentraScreenActivity;
import dev.identra.app.ui.IdentraSheet;
import dev.identra.app.x.core.hook.AppXpPacket;

/**
 * 0.4.0 profile editor: header with device / country / usage, then one collapsible card per
 * section (Device, Identifiers, Phone & SIM, Network, Location & region, Display & hardware,
 * Apps & accounts). Every value can be edited, re-randomized on its own or per section, and
 * locked so "Randomize all" keeps it. Changes are saved to the profile immediately; apps that
 * use the profile are only rewritten after the user confirms "Update apps".
 */
public class ProfileEditorActivity extends IdentraScreenActivity {
    private static final String EXTRA_ID = "identra.profile_id";
    private static final int M_RENAME = 1, M_DUPLICATE = 2, M_GLOBAL = 3, M_EXPORT = 4, M_DELETE = 5;

    public static Intent intent(Context c, String id) {
        Intent i = new Intent(c, ProfileEditorActivity.class);
        i.putExtra(EXTRA_ID, id);
        return i;
    }

    private Profile profile;
    /** Values as they were when the apps using the profile were last written. */
    private final Map<String, String> applied = new HashMap<>();
    private LinearLayout body;
    private final Set<ProfileKeys.Section> expanded = EnumSet.of(ProfileKeys.Section.DEVICE);
    private ActivityResultLauncher<String> exportLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        exportLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), this::onExport);
        profile = ProfileStore.get(this).byId(getIntent().getStringExtra(EXTRA_ID));
        if (profile == null) { finish(); return; }
        applied.putAll(profile.values);
        setTitle(profile.name);
        body = useScrollBody();

        Menu m = toolbar.getMenu();
        m.add(0, M_RENAME, 1, R.string.identra_prof_rename);
        m.add(0, M_DUPLICATE, 2, R.string.identra_prof_duplicate);
        m.add(0, M_GLOBAL, 3, R.string.identra_prof_global_set);
        m.add(0, M_EXPORT, 4, R.string.identra_prof_export);
        m.add(0, M_DELETE, 5, R.string.identra_prof_delete);
        toolbar.setOnMenuItemClickListener(item -> { onMenu(item.getItemId()); return true; });

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { leave(); }
        });
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (profile != null && ProfileStore.get(this).byId(profile.id) == null) { finish(); return; }
        if (body != null) render();
    }

    private boolean dirty() { return !applied.equals(profile.values); }

    private int users() {
        ProfileStore s = ProfileStore.get(this);
        return s.usersOf(profile.id).size() + (profile.id.equals(s.globalId()) ? 1 : 0);
    }

    private void leave() {
        if (dirty() && users() > 0) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.identra_prof_update_title)
                    .setMessage(getString(R.string.identra_prof_update_desc, users()))
                    .setNegativeButton(R.string.identra_prof_update_later, (d, w) -> finish())
                    .setPositiveButton(R.string.identra_prof_update_now, (d, w) -> updateApps(true))
                    .show();
            return;
        }
        finish();
    }

    private void save() {
        ProfileStore.get(this).put(profile);
    }

    // ---- rendering --------------------------------------------------------------------------

    private void render() {
        setTitle(profile.name);
        body.removeAllViews();
        updateMenuTitle();

        // header
        LinearLayout head = IdentraRows.card(body);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = IdentraRows.dp(this, 18);
        box.setPadding(p, p, p, p);
        String dev = profile.deviceSummary();
        box.addView(text(dev.isEmpty() ? getString(R.string.identra_prof_no_device) : dev, R.style.TextAppearance_Identra_CardTitle));
        String country = profile.countryLabel();
        if (!country.isEmpty()) box.addView(text(country, R.style.TextAppearance_Identra_RowSub));
        int filled = profile.filledCount(), total = ProfileKeys.names().size();
        ProfileStore store = ProfileStore.get(this);
        int apps = store.usersOf(profile.id).size();
        String usage = getString(R.string.identra_prof_values_set, filled, total) + " · " + (apps == 1 ? "1 app" : apps + " apps")
                + (profile.id.equals(store.globalId()) ? " · " + getString(R.string.identra_prof_default_badge) : "");
        box.addView(text(usage, R.style.TextAppearance_Identra_Caption));
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, IdentraRows.dp(this, 12), 0, 0);
        MaterialButton apply = button(R.layout.item_identra_button_filled, getString(R.string.identra_prof_apply_to_apps));
        apply.setOnClickListener(v -> ProfileUi.applyToApps(this, profile, this::afterApply));
        MaterialButton rnd = button(R.layout.item_identra_button_tonal, getString(R.string.identra_prof_randomize_all));
        rnd.setOnClickListener(v -> randomize(null, getString(R.string.identra_prof_randomized_all)));
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp1.setMarginEnd(IdentraRows.dp(this, 6));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp2.setMarginStart(IdentraRows.dp(this, 6));
        buttons.addView(apply, lp1);
        buttons.addView(rnd, lp2);
        box.addView(buttons);
        head.addView(box);

        // pending update
        if (dirty() && users() > 0) {
            LinearLayout upd = IdentraRows.card(body);
            View row = IdentraRows.row(upd, R.drawable.ic_identra_restart, getString(R.string.identra_prof_update_title),
                    getString(R.string.identra_prof_update_desc, users()), v -> updateApps(false));
            row.setBackgroundResource(R.color.identra_container);
        }

        for (ProfileKeys.Section s : ProfileKeys.Section.values()) renderSection(s);
        renderUsers();
    }

    private void updateMenuTitle() {
        setMenuTitle(M_GLOBAL, profile.id.equals(ProfileStore.get(this).globalId()) ? R.string.identra_prof_global_remove : R.string.identra_prof_global_set);
    }

    private void setMenuTitle(int id, int title) {
        android.view.MenuItem it = toolbar.getMenu().findItem(id);
        if (it != null) it.setTitle(title);
    }

    private TextView text(CharSequence t, int style) {
        TextView tv = new TextView(this);
        tv.setTextAppearance(style);
        tv.setText(t);
        return tv;
    }

    private MaterialButton button(int layout, CharSequence t) {
        MaterialButton b = (MaterialButton) getLayoutInflater().inflate(layout, body, false);
        b.setText(t);
        return b;
    }

    private void renderSection(final ProfileKeys.Section s) {
        List<ProfileKeys.Key> keys = ProfileKeys.of(s);
        int set = 0;
        for (ProfileKeys.Key k : keys) if (profile.has(k.name)) set++;
        final boolean open = expanded.contains(s);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(IdentraRows.dp(this, 4), IdentraRows.dp(this, 18), 0, IdentraRows.dp(this, 4));
        header.setBackgroundResource(selectable());
        ImageView chev = new ImageView(this);
        chev.setImageResource(R.drawable.ic_identra_chevron);
        chev.setRotation(open ? 90f : 0f);
        header.addView(chev, new LinearLayout.LayoutParams(IdentraRows.dp(this, 20), IdentraRows.dp(this, 20)));
        TextView title = text(getString(s.title), R.style.TextAppearance_Identra_Section);
        title.setPadding(IdentraRows.dp(this, 8), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(text(set + "/" + keys.size(), R.style.TextAppearance_Identra_Caption));
        MaterialButton dice = iconButton(R.drawable.ic_identra_dice, getString(R.string.identra_prof_randomize_section));
        dice.setOnClickListener(v -> {
            List<String> names = new ArrayList<>();
            for (ProfileKeys.Key k : ProfileKeys.of(s)) names.add(k.name);
            randomize(names, getString(R.string.identra_prof_randomized_section, getString(s.title)));
        });
        header.addView(dice);
        header.setOnClickListener(v -> {
            if (open) expanded.remove(s); else expanded.add(s);
            render();
        });
        body.addView(header);
        if (!open) return;

        LinearLayout card = IdentraRows.card(body);
        if (s == ProfileKeys.Section.REGION) {
            IdentraRows.row(card, R.drawable.ic_identra_globe, getString(R.string.identra_prof_change_country),
                    getString(R.string.identra_prof_change_country_sub), v -> changeCountry());
            IdentraRows.row(card, R.drawable.ic_identra_location, getString(R.string.identra_prof_use_place),
                    getString(R.string.identra_prof_use_place_sub), v -> pickPlace());
        }
        for (ProfileKeys.Key k : keys) card.addView(fieldRow(k));
    }

    private View fieldRow(final ProfileKeys.Key k) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(IdentraRows.dp(this, 18), IdentraRows.dp(this, 8), IdentraRows.dp(this, 4), IdentraRows.dp(this, 8));
        row.setBackgroundResource(selectable());
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(k.label, R.style.TextAppearance_Identra_Caption));
        TextView value;
        if (profile.has(k.name)) {
            value = text(profile.get(k.name), R.style.TextAppearance_Identra_RowTitle);
            value.setTextIsSelectable(false);
        } else {
            value = text(getString(R.string.identra_prof_not_set), R.style.TextAppearance_Identra_RowSub);
        }
        value.setMaxLines(2);
        value.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(value);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final boolean locked = profile.isLocked(k.name);
        MaterialButton lock = iconButton(locked ? R.drawable.ic_identra_lock : R.drawable.ic_identra_lock_open,
                getString(locked ? R.string.identra_prof_unlock : R.string.identra_prof_lock));
        lock.setAlpha(locked ? 1f : 0.45f);
        lock.setOnClickListener(v -> {
            profile.setLocked(k.name, !locked);
            save();
            render();
        });
        row.addView(lock);
        row.setOnClickListener(v -> edit(k));
        row.setOnLongClickListener(v -> {
            if (profile.has(k.name)) IdentraSheet.copy(this, k.label, profile.get(k.name));
            return true;
        });
        return row;
    }

    private MaterialButton iconButton(int icon, CharSequence desc) {
        MaterialButton b = (MaterialButton) getLayoutInflater().inflate(R.layout.item_identra_icon_button, body, false);
        b.setIconResource(icon);
        b.setContentDescription(desc);
        return b;
    }

    private int selectable() {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        return tv.resourceId;
    }

    private void renderUsers() {
        final ProfileStore store = ProfileStore.get(this);
        List<ProfileStore.Assignment> users = store.usersOf(profile.id);
        IdentraRows.section(body, getString(R.string.identra_prof_used_by));
        LinearLayout card = IdentraRows.card(body);
        IdentraData.Snapshot s = IdentraData.cached();
        for (final ProfileStore.Assignment a : users) {
            final AppXpPacket app = IdentraData.findApp(s, a.pkg, a.uid);
            String label = app != null ? IdentraData.labelOf(app) : a.pkg;
            String sub = a.pkg + " · " + dataLabel(a.dataMode);
            IdentraRows.row(card, R.drawable.ic_identra_apps, label, sub,
                    app == null ? null : (View.OnClickListener) v -> startActivity(AppDetailActivity.intent(this, app)));
        }
        IdentraRows.row(card, R.drawable.ic_identra_add, getString(R.string.identra_prof_apply_to_apps), null,
                v -> ProfileUi.applyToApps(this, profile, this::afterApply));
    }

    private String dataLabel(String mode) {
        if (ProfileStore.DATA_SEPARATE.equals(mode)) return getString(R.string.identra_prof_data_separate_short);
        if (ProfileStore.DATA_FRESH.equals(mode)) return getString(R.string.identra_prof_data_fresh_short);
        return getString(R.string.identra_prof_data_shared_short);
    }

    // ---- editing ----------------------------------------------------------------------------

    private void edit(final ProfileKeys.Key k) {
        final IdentraSheet sheet = new IdentraSheet(this).title(k.label).sub(k.name);
        final TextInputEditText et = sheet.input(getString(R.string.identra_prof_value), profile.get(k.name), "user.agent".equals(k.name) || k.name.contains("fingerprint"));
        sheet.text(getString(R.string.identra_prof_empty_hint));
        sheet.action(IdentraSheet.STYLE_FILLED, getString(R.string.identra_prof_save), v -> {
            String val = et.getText() == null ? "" : et.getText().toString();
            if (val.trim().isEmpty() && k.name.startsWith("location.l")) { /* allowed: clears */ }
            profile.put(k.name, val);
            save();
            render();
        }, true);
        sheet.action(IdentraSheet.STYLE_TONAL, getString(R.string.identra_prof_randomize), v -> {
            Profile tmp = profile.copy(profile.name);
            tmp.locked.clear();
            List<String> one = new ArrayList<>();
            one.add(k.name);
            ProfileGenerator.randomize(getApplicationContext(), tmp, one, null, false);
            et.setText(tmp.get(k.name));
            if (tmp.get(k.name).isEmpty()) snack(getString(R.string.identra_prof_no_generator));
        }, false);
        sheet.action(IdentraSheet.STYLE_TEXT, getString(R.string.identra_prof_clear), v -> {
            profile.put(k.name, null);
            save();
            render();
        }, true);
        sheet.show();
    }

    /** Re-randomize keys (null = all), keeping locked ones; offers Undo. */
    private void randomize(List<String> keys, final String done) {
        final Map<String, String> before = new HashMap<>(profile.values);
        final String beforeCountry = profile.country;
        ProfileGenerator.randomize(getApplicationContext(), profile, keys, null, true);
        save();
        render();
        Snackbar.make(findViewById(R.id.identraScreenRoot), done, Snackbar.LENGTH_LONG)
                .setAction(R.string.identra_prof_undo, v -> {
                    profile.values.clear();
                    profile.values.putAll(before);
                    profile.country = beforeCountry;
                    save();
                    render();
                }).show();
    }

    private void changeCountry() {
        ProfileUi.pickCountry(this, c -> {
            List<String> keys = new ArrayList<>();
            for (ProfileKeys.Key k : ProfileKeys.of(ProfileKeys.Section.REGION)) keys.add(k.name);
            for (ProfileKeys.Key k : ProfileKeys.of(ProfileKeys.Section.PHONE)) keys.add(k.name);
            keys.add("unique.network.ssid");
            keys.remove(ProfileKeys.LOC_ACC);
            final Map<String, String> before = new HashMap<>(profile.values);
            final String beforeCountry = profile.country;
            ProfileGenerator.randomize(getApplicationContext(), profile, keys, c.iso2, true);
            profile.country = c.iso2;
            save();
            render();
            Snackbar.make(findViewById(R.id.identraScreenRoot), getString(R.string.identra_prof_country_changed, c.name()), Snackbar.LENGTH_LONG)
                    .setAction(R.string.identra_prof_undo, v -> {
                        profile.values.clear();
                        profile.values.putAll(before);
                        profile.country = beforeCountry;
                        save();
                        render();
                    }).show();
        });
    }

    private void pickPlace() {
        final Context app = getApplicationContext();
        setBusy(true);
        IdentraData.EXECUTOR.submit(() -> {
            final IdentraLocation.Value g = IdentraLocation.readGlobal(app);
            main.post(() -> {
                setBusy(false);
                if (isFinishing()) return;
                final IdentraSheet sheet = new IdentraSheet(this).title(getString(R.string.identra_prof_use_place));
                LinearLayout c = IdentraRows.card(sheet.content());
                int n = 0;
                if (g != null && g.set) {
                    String name = IdentraPlaces.nameFor(this, null, 0, g.lat, g.lon);
                    final IdentraPlaces.Place gp = new IdentraPlaces.Place(name != null ? name : getString(R.string.identra_prof_global_location), null, g.lat, g.lon);
                    IdentraRows.row(c, R.drawable.ic_identra_globe, getString(R.string.identra_prof_global_location), gp.name + " · " + IdentraPlaces.shortCoords(g.lat, g.lon), v -> { sheet.dismiss(); usePlace(gp); });
                    n++;
                }
                for (final IdentraPlaces.Place p : IdentraPlaces.favourites(this)) {
                    IdentraRows.row(c, R.drawable.ic_identra_star, p.name, IdentraPlaces.shortCoords(p.lat, p.lon), v -> { sheet.dismiss(); usePlace(p); });
                    n++;
                }
                for (final IdentraPlaces.Place p : IdentraPlaces.recent(this)) {
                    IdentraRows.row(c, R.drawable.ic_identra_history, p.name, IdentraPlaces.shortCoords(p.lat, p.lon), v -> { sheet.dismiss(); usePlace(p); });
                    n++;
                }
                if (n == 0) sheet.text(getString(R.string.identra_prof_no_places));
                sheet.show();
            });
        });
    }

    private void usePlace(IdentraPlaces.Place p) {
        profile.put(ProfileKeys.LOC_TYPE, IdentraLocation.TYPE_SET);
        profile.put(ProfileKeys.LOC_LAT, IdentraLocation.format(p.lat));
        profile.put(ProfileKeys.LOC_LON, IdentraLocation.format(p.lon));
        save();
        render();
        snack(getString(R.string.identra_prof_place_set, p.name));
    }

    // ---- apps using the profile -------------------------------------------------------------

    private void afterApply() {
        applied.clear();
        applied.putAll(profile.values);
        render();
    }

    private void updateApps(final boolean finishAfter) {
        final Profile before = new Profile();
        before.values.putAll(applied);
        final ProfileUi.Busy b = ProfileUi.busy(this, getString(R.string.identra_prof_updating));
        final Context ctx = getApplicationContext();
        ProfileApplier.EXEC.submit(() -> {
            final ProfileApplier.Result r = ProfileApplier.reapplyEverywhere(ctx, profile, before, b::step);
            b.dismiss();
            main.post(() -> {
                applied.clear();
                applied.putAll(profile.values);
                if (finishAfter) { finish(); return; }
                if (isFinishing()) return;
                render();
                snack(r.failed == 0 ? getString(R.string.identra_prof_updated) : getString(R.string.identra_prof_updated_failed, r.failed));
            });
        });
    }

    // ---- menu -------------------------------------------------------------------------------

    private void onMenu(int id) {
        switch (id) {
            case M_RENAME: ProfileUi.rename(this, profile, this::render); break;
            case M_DUPLICATE: {
                ProfileStore store = ProfileStore.get(this);
                Profile copy = profile.copy(store.uniqueName(profile.name + " copy"));
                store.put(copy);
                startActivity(intent(this, copy.id));
                break;
            }
            case M_GLOBAL: ProfileUi.toggleGlobal(this, profile, () -> { afterGlobal(); render(); }); break;
            case M_EXPORT: exportLauncher.launch(profile.name.replaceAll("[^A-Za-z0-9._-]+", "_") + ".json"); break;
            case M_DELETE: ProfileUi.delete(this, profile, this::finish); break;
            default: break;
        }
    }

    private void afterGlobal() {
        if (ProfileStore.get(this).usersOf(profile.id).isEmpty()) { applied.clear(); applied.putAll(profile.values); }
    }

    private void onExport(@Nullable Uri uri) {
        if (uri == null) return;
        final Context c = getApplicationContext();
        final List<Profile> one = new ArrayList<>();
        one.add(profile);
        IdentraData.EXECUTOR.submit(() -> {
            String err = null;
            try (OutputStream os = c.getContentResolver().openOutputStream(uri, "wt")) {
                if (os == null) throw new IllegalStateException("cannot open file");
                os.write(ProfileStore.export(one).getBytes(StandardCharsets.UTF_8));
            } catch (Throwable t) {
                err = String.valueOf(t.getMessage());
            }
            final String fErr = err;
            main.post(() -> snack(fErr == null ? getString(R.string.identra_prof_exported, 1) : getString(R.string.identra_prof_export_failed, fErr)));
        });
    }
}
