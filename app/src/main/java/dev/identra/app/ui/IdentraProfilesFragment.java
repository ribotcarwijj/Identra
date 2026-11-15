package dev.identra.app.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import dev.identra.app.R;
import dev.identra.app.ui.profiles.Profile;
import dev.identra.app.ui.profiles.ProfileEditorActivity;
import dev.identra.app.ui.profiles.ProfileStore;
import dev.identra.app.ui.profiles.ProfileUi;

/** 0.4.0 Profiles tab: complete device identities, the global default, create / import / export. */
public class IdentraProfilesFragment extends Fragment {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final IdentraListAdapter adapter = new IdentraListAdapter();
    private View root;
    private ActivityResultLauncher<String[]> importLauncher;
    private ActivityResultLauncher<String> exportLauncher;
    private List<Profile> pendingExport;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        importLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onImport);
        exportLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), this::onExport);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_identra_profiles, container, false);
        RecyclerView rv = root.findViewById(R.id.profList);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);
        root.findViewById(R.id.profNew).setOnClickListener(v -> ProfileUi.create(requireActivity(), this::open));
        root.findViewById(R.id.profImport).setOnClickListener(v -> importLauncher.launch(new String[]{"application/json", "text/plain", "*/*"}));
        root.findViewById(R.id.profExport).setOnClickListener(v -> {
            List<Profile> all = ProfileStore.get(requireContext()).list();
            if (all.isEmpty()) { snack(getString(R.string.identra_prof_nothing_to_export)); return; }
            exportAs(all, "identra-profiles.json");
        });
        render();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) render();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) render();
    }

    private void open(Profile p) {
        if (!isAdded()) return;
        startActivity(ProfileEditorActivity.intent(requireContext(), p.id));
    }

    void render() {
        if (root == null || !isAdded()) return;
        final Context c = requireContext();
        ProfileStore store = ProfileStore.get(c);
        List<Profile> list = store.list();
        List<IdentraListAdapter.Item> items = new ArrayList<>();
        Profile global = store.global();
        if (!list.isEmpty()) {
            items.add(IdentraListAdapter.Item.header(getString(R.string.identra_prof_global_header)));
            items.add(IdentraListAdapter.Item.row(R.drawable.ic_identra_globe,
                            global == null ? getString(R.string.identra_prof_global_none) : global.name,
                            getString(global == null ? R.string.identra_prof_global_none_sub : R.string.identra_prof_global_sub))
                    .click(v -> ProfileUi.pickProfile(requireActivity(), getString(R.string.identra_prof_global_header),
                            global == null ? null : getString(R.string.identra_prof_global_clear), p -> {
                                if (p == null) { if (global != null) ProfileUi.toggleGlobal(requireActivity(), global, this::render); }
                                else if (global == null || !p.id.equals(global.id)) ProfileUi.toggleGlobal(requireActivity(), p, this::render);
                            })));
            items.add(IdentraListAdapter.Item.header(getString(R.string.identra_prof_yours)));
        }
        for (final Profile p : list) {
            IdentraListAdapter.Item it = IdentraListAdapter.Item.row(R.drawable.ic_identra_device, p.name, ProfileUi.summary(c, p))
                    .click(v -> open(p))
                    .longClick(v -> { actions(p); return true; });
            if (global != null && global.id.equals(p.id)) it.trailing(getString(R.string.identra_prof_default_badge));
            items.add(it);
        }
        adapter.submit(items);
        View empty = root.findViewById(R.id.profEmpty);
        empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        if (list.isEmpty()) {
            ((ImageView) empty.findViewById(R.id.identraEmptyArt)).setImageResource(R.drawable.ill_identra_profiles);
            ((TextView) empty.findViewById(R.id.identraEmptyTitle)).setText(R.string.identra_prof_empty_title);
            TextView b = empty.findViewById(R.id.identraEmptyBody);
            b.setText(R.string.identra_prof_empty_body);
            b.setVisibility(View.VISIBLE);
        }
    }

    private void actions(final Profile p) {
        IdentraSheet sheet = new IdentraSheet(requireContext()).title(p.name).sub(ProfileUi.summary(requireContext(), p));
        android.widget.LinearLayout c = IdentraRows.card(sheet.content());
        IdentraRows.row(c, R.drawable.ic_identra_apps, getString(R.string.identra_prof_apply_to_apps), null, v -> {
            sheet.dismiss();
            ProfileUi.applyToApps(requireActivity(), p, this::render);
        });
        boolean isGlobal = p.id.equals(ProfileStore.get(requireContext()).globalId());
        IdentraRows.row(c, R.drawable.ic_identra_globe, getString(isGlobal ? R.string.identra_prof_global_remove : R.string.identra_prof_global_set), null, v -> {
            sheet.dismiss();
            ProfileUi.toggleGlobal(requireActivity(), p, this::render);
        });
        IdentraRows.row(c, R.drawable.ic_identra_edit, getString(R.string.identra_prof_rename), null, v -> {
            sheet.dismiss();
            ProfileUi.rename(requireActivity(), p, this::render);
        });
        IdentraRows.row(c, R.drawable.ic_identra_copy, getString(R.string.identra_prof_duplicate), null, v -> {
            sheet.dismiss();
            ProfileStore store = ProfileStore.get(requireContext());
            Profile copy = p.copy(store.uniqueName(p.name + " copy"));
            store.put(copy);
            render();
            open(copy);
        });
        IdentraRows.row(c, R.drawable.ic_identra_upload, getString(R.string.identra_prof_export), null, v -> {
            sheet.dismiss();
            List<Profile> one = new ArrayList<>();
            one.add(p);
            exportAs(one, p.name.replaceAll("[^A-Za-z0-9._-]+", "_") + ".json");
        });
        IdentraRows.row(c, R.drawable.ic_identra_delete, getString(R.string.identra_prof_delete), null, v -> {
            sheet.dismiss();
            ProfileUi.delete(requireActivity(), p, this::render);
        });
        sheet.show();
    }

    // ---- import / export ----

    private void exportAs(List<Profile> list, String name) {
        pendingExport = list;
        exportLauncher.launch(name);
    }

    private void onExport(Uri uri) {
        final List<Profile> list = pendingExport;
        pendingExport = null;
        if (uri == null || list == null || !isAdded()) return;
        final Context c = requireContext().getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            String err = null;
            try (OutputStream os = c.getContentResolver().openOutputStream(uri, "wt")) {
                if (os == null) throw new IllegalStateException("cannot open file");
                os.write(ProfileStore.export(list).getBytes(StandardCharsets.UTF_8));
            } catch (Throwable t) {
                err = String.valueOf(t.getMessage());
            }
            final String fErr = err;
            main.post(() -> {
                if (!isAdded()) return;
                snack(fErr == null ? getString(R.string.identra_prof_exported, list.size()) : getString(R.string.identra_prof_export_failed, fErr));
            });
        });
    }

    private void onImport(Uri uri) {
        if (uri == null || !isAdded()) return;
        final Context c = requireContext().getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            String err = null;
            int n = 0;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                if (in == null) throw new IllegalStateException("cannot open file");
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) > 0) {
                    bo.write(buf, 0, r);
                    if (bo.size() > 4 * 1024 * 1024) throw new IllegalStateException("file too large");
                }
                n = ProfileStore.get(c).importJson(new String(bo.toByteArray(), StandardCharsets.UTF_8)).size();
            } catch (Throwable t) {
                err = String.valueOf(t.getMessage());
            }
            final String fErr = err;
            final int fN = n;
            main.post(() -> {
                if (!isAdded()) return;
                render();
                if (fErr == null) snack(getString(R.string.identra_prof_imported, fN));
                else new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.identra_prof_import_failed)
                        .setMessage(fErr).setPositiveButton(android.R.string.ok, null).show();
            });
        });
    }

    private void snack(String text) {
        View v = getView();
        if (v != null) Snackbar.make(v, text, Snackbar.LENGTH_LONG).show();
    }
}
