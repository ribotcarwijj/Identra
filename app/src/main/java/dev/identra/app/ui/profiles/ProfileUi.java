package dev.identra.app.ui.profiles;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import dev.identra.app.R;
import dev.identra.app.ui.IdentraData;
import dev.identra.app.ui.IdentraListAdapter;
import dev.identra.app.ui.IdentraRows;
import dev.identra.app.ui.IdentraSheet;
import dev.identra.app.x.core.hook.AppXpPacket;

/** 0.4.0: shared profile dialogs (create, pick, apply with data options, progress). */
public final class ProfileUi {
    private ProfileUi() { }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Done { void done(); }

    // ---- progress ------------------------------------------------------------------------

    public static final class Busy {
        private final AlertDialog dialog;
        private final TextView message;

        Busy(Context c, CharSequence title) {
            LinearLayout box = new LinearLayout(c);
            box.setOrientation(LinearLayout.VERTICAL);
            int p = IdentraRows.dp(c, 24);
            box.setPadding(p, IdentraRows.dp(c, 8), p, 0);
            message = new TextView(c);
            message.setTextAppearance(R.style.TextAppearance_Identra_RowSub);
            LinearProgressIndicator bar = new LinearProgressIndicator(c);
            bar.setIndeterminate(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = IdentraRows.dp(c, 16);
            box.addView(message);
            box.addView(bar, lp);
            dialog = new MaterialAlertDialogBuilder(c).setTitle(title).setView(box).setCancelable(false).create();
            dialog.show();
        }

        public void step(String text) { MAIN.post(() -> message.setText(text)); }

        public void dismiss() { MAIN.post(() -> { try { dialog.dismiss(); } catch (Throwable ignored) { } }); }
    }

    public static Busy busy(Context c, CharSequence title) { return new Busy(c, title); }

    private static void info(Context c, CharSequence title, CharSequence body) {
        new MaterialAlertDialogBuilder(c).setTitle(title).setMessage(body).setPositiveButton(android.R.string.ok, null).show();
    }

    // ---- create --------------------------------------------------------------------------

    public static void create(final Activity a, final Consumer<Profile> created) {
        IdentraSheet sheet = new IdentraSheet(a).title(a.getString(R.string.identra_prof_new)).sub(a.getString(R.string.identra_prof_new_sub));
        LinearLayout c = IdentraRows.card(sheet.content());
        IdentraRows.row(c, R.drawable.ic_identra_dice, a.getString(R.string.identra_prof_new_random), a.getString(R.string.identra_prof_new_random_sub), v -> {
            sheet.dismiss();
            generate(a, null, false, created);
        });
        IdentraRows.row(c, R.drawable.ic_identra_globe, a.getString(R.string.identra_prof_new_country), a.getString(R.string.identra_prof_new_country_sub), v -> {
            sheet.dismiss();
            pickCountry(a, country -> generate(a, country.iso2, false, created));
        });
        IdentraRows.row(c, R.drawable.ic_identra_device, a.getString(R.string.identra_prof_new_device), a.getString(R.string.identra_prof_new_device_sub), v -> {
            sheet.dismiss();
            generate(a, null, true, created);
        });
        if (ProfileStore.get(a).count() > 0)
            IdentraRows.row(c, R.drawable.ic_identra_copy, a.getString(R.string.identra_prof_new_duplicate), a.getString(R.string.identra_prof_new_duplicate_sub), v -> {
                sheet.dismiss();
                pickProfile(a, a.getString(R.string.identra_prof_new_duplicate), null, p -> {
                    if (p == null) return;
                    ProfileStore store = ProfileStore.get(a);
                    Profile copy = p.copy(store.uniqueName(p.name + " copy"));
                    store.put(copy);
                    created.accept(copy);
                });
            });
        IdentraRows.row(c, R.drawable.ic_identra_edit, a.getString(R.string.identra_prof_new_empty), a.getString(R.string.identra_prof_new_empty_sub), v -> {
            sheet.dismiss();
            ProfileStore store = ProfileStore.get(a);
            Profile p = Profile.create(store.uniqueName("Profile"));
            store.put(p);
            created.accept(p);
        });
        sheet.show();
    }

    private static void generate(final Activity a, final String countryIso, final boolean fromDevice, final Consumer<Profile> created) {
        final Context app = a.getApplicationContext();
        ProfileApplier.EXEC.submit(() -> {
            ProfileStore store = ProfileStore.get(app);
            Profile p;
            if (fromDevice) {
                p = Profile.create(store.uniqueName("This phone"));
                ProfileGenerator.fromThisDevice(app, p);
            } else {
                p = Profile.create("Profile");
                ProfileGenerator.randomize(app, p, null, countryIso, false);
                String dev = p.deviceSummary();
                CountryCatalog.Country c = CountryCatalog.byIso(p.country);
                p.name = store.uniqueName((dev.isEmpty() ? "Profile" : dev) + (c != null ? " · " + c.iso2 : ""));
            }
            store.put(p);
            final Profile fp = p;
            MAIN.post(() -> { if (!a.isFinishing()) created.accept(fp); });
        });
    }

    public static void pickCountry(final Context ctx, final Consumer<CountryCatalog.Country> picked) {
        final IdentraSheet sheet = new IdentraSheet(ctx).title(ctx.getString(R.string.identra_prof_pick_country));
        LinearLayout c = IdentraRows.card(sheet.content());
        for (final CountryCatalog.Country co : CountryCatalog.all()) {
            StringBuilder ops = new StringBuilder();
            for (CountryCatalog.Operator o : co.operators) { if (ops.length() > 0) ops.append(", "); ops.append(o.name); }
            IdentraRows.row(c, 0, co.flag() + "  " + co.name(), ops, v -> { sheet.dismiss(); picked.accept(co); });
        }
        sheet.show();
    }

    /** Pick one profile; {@code noneLabel} adds a "none" row returning null. */
    public static void pickProfile(final Context ctx, CharSequence title, String noneLabel, final Consumer<Profile> picked) {
        final IdentraSheet sheet = new IdentraSheet(ctx).title(title);
        LinearLayout c = IdentraRows.card(sheet.content());
        if (noneLabel != null) IdentraRows.row(c, R.drawable.ic_identra_close, noneLabel, null, v -> { sheet.dismiss(); picked.accept(null); });
        for (final Profile p : ProfileStore.get(ctx).list())
            IdentraRows.row(c, R.drawable.ic_identra_device, p.name, summary(ctx, p), v -> { sheet.dismiss(); picked.accept(p); });
        sheet.show();
    }

    public static String summary(Context ctx, Profile p) {
        List<String> parts = new ArrayList<>();
        String dev = p.deviceSummary();
        if (!dev.isEmpty()) parts.add(dev);
        String country = p.countryLabel();
        if (!country.isEmpty()) parts.add(country);
        int users = ProfileStore.get(ctx).usersOf(p.id).size();
        parts.add(users == 1 ? "1 app" : users + " apps");
        if (p.id.equals(ProfileStore.get(ctx).globalId())) parts.add("global default");
        return android.text.TextUtils.join(" · ", parts);
    }

    // ---- choose for one app (App detail chip) ------------------------------------------

    public static void chooseForApp(final Activity a, final AppXpPacket app, final Done done) {
        final ProfileStore store = ProfileStore.get(a);
        final Profile current = store.profileFor(app.packageName, app.uid);
        final IdentraSheet sheet = new IdentraSheet(a).title(a.getString(R.string.identra_prof_for_app, IdentraData.labelOf(app)))
                .sub(current == null ? a.getString(R.string.identra_prof_for_app_none) : a.getString(R.string.identra_prof_for_app_current, current.name));
        LinearLayout c = IdentraRows.card(sheet.content());
        for (final Profile p : store.list()) {
            boolean isCur = current != null && current.id.equals(p.id);
            View row = IdentraRows.row(c, isCur ? R.drawable.ic_identra_check : R.drawable.ic_identra_device, p.name, summary(a, p), v -> {
                sheet.dismiss();
                List<AppXpPacket> one = new ArrayList<>();
                one.add(app);
                applyOptions(a, p, one, done);
            });
            if (isCur) row.setAlpha(1f);
        }
        LinearLayout c2 = IdentraRows.card(sheet.content());
        IdentraRows.row(c2, R.drawable.ic_identra_dice, a.getString(R.string.identra_prof_new_random_for_app), null, v -> {
            sheet.dismiss();
            generate(a, current != null ? current.countryIso() : null, false, p -> {
                List<AppXpPacket> one = new ArrayList<>();
                one.add(app);
                applyOptions(a, p, one, done);
            });
        });
        if (current != null)
            IdentraRows.row(c2, R.drawable.ic_identra_close, a.getString(R.string.identra_prof_remove_from_app), a.getString(R.string.identra_prof_remove_from_app_sub), v -> {
                sheet.dismiss();
                new MaterialAlertDialogBuilder(a)
                        .setTitle(a.getString(R.string.identra_prof_remove_from_app))
                        .setMessage(a.getString(R.string.identra_prof_remove_confirm, current.name, IdentraData.labelOf(app)))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.identra_prof_remove, (d, w) -> {
                            final Busy b = busy(a, a.getString(R.string.identra_prof_removing));
                            final Context ctx = a.getApplicationContext();
                            ProfileApplier.EXEC.submit(() -> {
                                ProfileApplier.removeFromApp(ctx, app);
                                b.dismiss();
                                MAIN.post(() -> { if (done != null) done.done(); });
                            });
                        }).show();
            });
        sheet.show();
    }

    // ---- apply to many apps (from a profile) --------------------------------------------

    public static void applyToApps(final Activity a, final Profile profile, final Done done) {
        final BottomSheetDialog dialog = new BottomSheetDialog(a);
        final LinearLayout root = (LinearLayout) LayoutInflater.from(a).inflate(R.layout.sheet_identra_app_picker, null, false);
        dialog.setContentView(root);
        try { dialog.getBehavior().setSkipCollapsed(true); dialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED); } catch (Throwable ignored) { }
        ((TextView) root.findViewById(R.id.identraPickerTitle)).setText(a.getString(R.string.identra_prof_apply_title, profile.name));
        final EditText search = root.findViewById(R.id.identraPickerSearch);
        final ChipGroup chips = root.findViewById(R.id.identraPickerChips);
        final TextView empty = root.findViewById(R.id.identraPickerEmpty);
        final View progress = root.findViewById(R.id.identraPickerProgress);
        RecyclerView rv = root.findViewById(R.id.identraPickerList);
        rv.setLayoutManager(new LinearLayoutManager(a));
        final IdentraListAdapter adapter = new IdentraListAdapter();
        rv.setAdapter(adapter);

        final Set<String> chosen = new LinkedHashSet<>();
        final ProfileStore store = ProfileStore.get(a);
        for (ProfileStore.Assignment as : store.usersOf(profile.id)) chosen.add(as.key());

        final MaterialButton go = (MaterialButton) LayoutInflater.from(a).inflate(R.layout.item_identra_button_filled, root, false);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) go.getLayoutParams();
        int m = IdentraRows.dp(a, 16);
        lp.setMargins(m, IdentraRows.dp(a, 8), m, m);
        root.addView(go, lp);

        final Runnable[] render = new Runnable[1];
        final Runnable updateButton = () -> {
            go.setEnabled(!chosen.isEmpty());
            go.setText(a.getString(R.string.identra_prof_apply_n, chosen.size()));
        };
        render[0] = () -> {
            IdentraData.Snapshot s = IdentraData.cached();
            if (s == null) return;
            String q = search.getText() == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
            int chip = chips.getCheckedChipId();
            List<IdentraListAdapter.Item> items = new ArrayList<>();
            for (final AppXpPacket app : s.apps) {
                boolean prot = IdentraData.isProtected(app);
                if (chip == R.id.identraPickerProtected && !prot) continue;
                if (chip == R.id.identraPickerUser && !(app.uid > Process.FIRST_APPLICATION_UID && !app.system)) continue;
                String label = IdentraData.labelOf(app);
                if (!q.isEmpty() && !label.toLowerCase(Locale.ROOT).contains(q) && !app.packageName.toLowerCase(Locale.ROOT).contains(q)) continue;
                final String key = ProfileStore.keyOf(app.packageName, app.uid);
                Profile cur = store.profileFor(app.packageName, app.uid);
                String sub = app.packageName + (cur != null ? " · " + cur.name : "");
                IdentraListAdapter.Item it = IdentraListAdapter.Item.row(0, label, sub).app(app.packageName, app.icon)
                        .toggle(chosen.contains(key), (item, on) -> {
                            if (on) chosen.add(key); else chosen.remove(key);
                            updateButton.run();
                        });
                items.add(it);
            }
            adapter.submit(items);
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        };
        chips.setOnCheckedStateChangeListener((g, ids) -> render[0].run());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int x, int y, int z) { }
            @Override public void onTextChanged(CharSequence s, int x, int y, int z) { }
            @Override public void afterTextChanged(Editable s) { render[0].run(); }
        });
        go.setOnClickListener(v -> {
            IdentraData.Snapshot s = IdentraData.cached();
            List<AppXpPacket> apps = new ArrayList<>();
            if (s != null) for (AppXpPacket app : s.apps) if (chosen.contains(ProfileStore.keyOf(app.packageName, app.uid))) apps.add(app);
            if (apps.isEmpty()) return;
            dialog.dismiss();
            applyOptions(a, profile, apps, done);
        });
        updateButton.run();
        IdentraData.ensure(a, s -> {
            progress.setVisibility(View.GONE);
            chips.check(IdentraData.protectedApps(s).isEmpty() ? R.id.identraPickerUser : R.id.identraPickerProtected);
            render[0].run();
        });
        dialog.show();
    }

    // ---- apply options + run ---------------------------------------------------------------

    public static void applyOptions(final Activity a, final Profile profile, final List<AppXpPacket> apps, final Done done) {
        final IdentraSheet sheet = new IdentraSheet(a);
        sheet.title(a.getString(R.string.identra_prof_apply_title, profile.name));
        sheet.sub(apps.size() == 1 ? IdentraData.labelOf(apps.get(0)) : a.getString(R.string.identra_prof_apply_to_n, apps.size()));
        LinearLayout content = sheet.content();

        TextView dataTitle = new TextView(a);
        dataTitle.setTextAppearance(R.style.TextAppearance_Identra_Section);
        dataTitle.setText(R.string.identra_prof_data_title);
        dataTitle.setPadding(0, IdentraRows.dp(a, 4), 0, IdentraRows.dp(a, 4));
        content.addView(dataTitle);

        final RadioGroup group = new RadioGroup(a);
        group.setOrientation(RadioGroup.VERTICAL);
        final MaterialRadioButton shared = radio(a, R.string.identra_prof_data_shared);
        final MaterialRadioButton fresh = radio(a, R.string.identra_prof_data_fresh);
        final MaterialRadioButton separate = radio(a, R.string.identra_prof_data_separate);
        group.addView(shared);
        group.addView(fresh);
        group.addView(separate);
        content.addView(group);
        final TextView explain = new TextView(a);
        explain.setTextAppearance(R.style.TextAppearance_Identra_RowSub);
        explain.setPadding(IdentraRows.dp(a, 4), 0, 0, IdentraRows.dp(a, 8));
        content.addView(explain);

        // preselect the mode the app already uses (single app)
        String prevMode = null;
        if (apps.size() == 1) {
            ProfileStore.Assignment as = ProfileStore.get(a).assignment(apps.get(0).packageName, apps.get(0).uid);
            if (as != null) prevMode = as.dataMode;
        }
        if (ProfileStore.DATA_SEPARATE.equals(prevMode)) separate.setChecked(true);
        else shared.setChecked(true);

        final Runnable describe = () -> {
            int id = group.getCheckedRadioButtonId();
            if (id == fresh.getId()) explain.setText(R.string.identra_prof_data_fresh_desc);
            else if (id == separate.getId()) explain.setText(R.string.identra_prof_data_separate_desc);
            else explain.setText(R.string.identra_prof_data_shared_desc);
        };
        describe.run();
        Boolean root = AppDataVault.cachedRoot();
        if (Boolean.FALSE.equals(root)) {
            separate.setEnabled(false);
            separate.setText(a.getString(R.string.identra_prof_data_separate) + " — " + a.getString(R.string.identra_prof_no_root_short));
        }
        group.setOnCheckedChangeListener((g, id) -> {
            describe.run();
            if (id == separate.getId() && AppDataVault.cachedRoot() == null) {
                explain.setText(R.string.identra_prof_checking_root);
                ProfileApplier.EXEC.submit(() -> {
                    final boolean ok = AppDataVault.hasRoot(false);
                    MAIN.post(() -> {
                        if (ok) { describe.run(); return; }
                        shared.setChecked(true);
                        separate.setEnabled(false);
                        separate.setText(a.getString(R.string.identra_prof_data_separate) + " — " + a.getString(R.string.identra_prof_no_root_short));
                        explain.setText(R.string.identra_prof_no_root);
                    });
                });
            }
        });

        final MaterialSwitch hooks = IdentraRows.switchRow(IdentraRows.card(content), R.drawable.ic_identra_shield,
                a.getString(R.string.identra_prof_enable_hooks), a.getString(R.string.identra_prof_enable_hooks_sub), true, null);

        sheet.action(IdentraSheet.STYLE_FILLED, a.getString(R.string.identra_prof_apply), v -> {
            int id = group.getCheckedRadioButtonId();
            final String mode = id == fresh.getId() ? ProfileStore.DATA_FRESH : id == separate.getId() ? ProfileStore.DATA_SEPARATE : ProfileStore.DATA_SHARED;
            final boolean enable = hooks.isChecked();
            String names = apps.size() == 1 ? IdentraData.labelOf(apps.get(0)) : a.getString(R.string.identra_prof_apply_to_n, apps.size());
            String msg;
            if (ProfileStore.DATA_FRESH.equals(mode)) msg = a.getString(R.string.identra_prof_confirm_fresh, names);
            else if (ProfileStore.DATA_SEPARATE.equals(mode)) msg = a.getString(R.string.identra_prof_confirm_separate, names, profile.name);
            else msg = a.getString(R.string.identra_prof_confirm_shared, profile.name, names);
            new MaterialAlertDialogBuilder(a)
                    .setTitle(a.getString(R.string.identra_prof_apply_title, profile.name))
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.identra_prof_apply, (d, w) -> run(a, profile, apps, mode, enable, done))
                    .show();
        }, true);
        sheet.action(IdentraSheet.STYLE_TEXT, a.getString(android.R.string.cancel), null, true);
        sheet.show();
    }

    private static MaterialRadioButton radio(Context c, int text) {
        MaterialRadioButton b = new MaterialRadioButton(c);
        b.setId(View.generateViewId());
        b.setText(text);
        b.setTextAppearance(R.style.TextAppearance_Identra_RowTitle);
        b.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(c, R.color.identra_primary)));
        return b;
    }

    private static void run(final Activity a, final Profile profile, final List<AppXpPacket> apps, final String mode,
                            final boolean enableHooks, final Done done) {
        final Busy b = busy(a, a.getString(R.string.identra_prof_applying, profile.name));
        final Context ctx = a.getApplicationContext();
        ProfileApplier.EXEC.submit(() -> {
            final StringBuilder report = new StringBuilder();
            int ok = 0;
            for (AppXpPacket app : apps) {
                final String label = IdentraData.labelOf(app);
                ProfileApplier.Result r = ProfileApplier.applyToApp(ctx, profile, app, mode, enableHooks,
                        text -> b.step(label + "\n" + text));
                if (r.ok) {
                    ok++;
                    report.append("✓ ").append(label).append(": ").append(r.written).append(" values");
                    if (r.hooks > 0) report.append(", ").append(r.hooks).append(" protection groups on");
                    if (r.dataNote != null) report.append(", ").append(r.dataNote);
                    if (r.failed > 0) report.append(" (").append(r.failed).append(" failed)");
                } else {
                    report.append("✗ ").append(label).append(": ").append(r.error);
                }
                report.append('\n');
            }
            b.dismiss();
            final int fOk = ok;
            MAIN.post(() -> {
                if (a.isFinishing()) { if (done != null) done.done(); return; }
                info(a, fOk == apps.size() ? a.getString(R.string.identra_prof_applied) : a.getString(R.string.identra_prof_applied_partly),
                        report.toString().trim());
                if (done != null) done.done();
            });
        });
    }

    // ---- global default ----------------------------------------------------------------------

    public static void toggleGlobal(final Activity a, final Profile p, final Done done) {
        final ProfileStore store = ProfileStore.get(a);
        final boolean isGlobal = p.id.equals(store.globalId());
        new MaterialAlertDialogBuilder(a)
                .setTitle(isGlobal ? R.string.identra_prof_global_remove : R.string.identra_prof_global_set)
                .setMessage(isGlobal ? a.getString(R.string.identra_prof_global_remove_desc, p.name) : a.getString(R.string.identra_prof_global_set_desc, p.name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    final Busy b = busy(a, a.getString(R.string.identra_prof_global_working));
                    final Context ctx = a.getApplicationContext();
                    ProfileApplier.EXEC.submit(() -> {
                        ProfileApplier.Result r = isGlobal ? ProfileApplier.clearGlobal(ctx) : ProfileApplier.applyGlobal(ctx, p);
                        b.dismiss();
                        MAIN.post(() -> {
                            if (!r.ok) info(a, a.getString(R.string.identra_prof_failed), r.error);
                            if (done != null) done.done();
                        });
                    });
                }).show();
    }

    // ---- rename / delete ------------------------------------------------------------------------

    public static void rename(final Activity a, final Profile p, final Done done) {
        IdentraSheet sheet = new IdentraSheet(a).title(a.getString(R.string.identra_prof_rename));
        final TextInputEditText et = sheet.input(a.getString(R.string.identra_prof_name), p.name, false);
        sheet.action(IdentraSheet.STYLE_FILLED, a.getString(R.string.identra_prof_save), v -> {
            String n = et.getText() == null ? "" : et.getText().toString().trim();
            if (n.isEmpty()) return;
            p.name = n;
            ProfileStore.get(a).put(p);
            if (done != null) done.done();
        }, true);
        sheet.show();
    }

    public static void delete(final Activity a, final Profile p, final Done done) {
        final ProfileStore store = ProfileStore.get(a);
        final List<ProfileStore.Assignment> users = store.usersOf(p.id);
        final boolean isGlobal = p.id.equals(store.globalId());
        String msg = a.getString(R.string.identra_prof_delete_desc, p.name);
        if (!users.isEmpty() || isGlobal) msg += "\n\n" + a.getString(R.string.identra_prof_delete_users, users.size());
        new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.identra_prof_delete)
                .setMessage(msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.identra_prof_delete, (d, w) -> {
                    final Busy b = busy(a, a.getString(R.string.identra_prof_deleting));
                    final Context ctx = a.getApplicationContext();
                    ProfileApplier.EXEC.submit(() -> {
                        IdentraData.Snapshot s = IdentraData.cached();
                        for (ProfileStore.Assignment as : users) {
                            AppXpPacket app = IdentraData.findApp(s, as.pkg, as.uid);
                            if (app == null) { app = new AppXpPacket(); app.packageName = as.pkg; app.uid = as.uid; }
                            b.step(as.pkg);
                            ProfileApplier.removeFromApp(ctx, app);
                        }
                        if (isGlobal) ProfileApplier.clearGlobal(ctx);
                        store.delete(p.id);
                        b.dismiss();
                        MAIN.post(() -> { if (done != null) done.done(); });
                    });
                }).show();
    }
}
