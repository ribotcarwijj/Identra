package dev.identra.app.ui;

import android.content.Context;
import android.os.Process;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.identra.app.R;
import dev.identra.app.x.core.hook.AppXpPacket;

/** Bottom-sheet app picker (protected apps first), shared by Location, Logs and Values. */
public final class IdentraAppPicker {
    public interface Picked { void onPicked(AppXpPacket app); }

    private IdentraAppPicker() { }

    public static void show(final Context context, CharSequence title, final Picked picked) {
        final BottomSheetDialog dialog = new BottomSheetDialog(context);
        View v = LayoutInflater.from(context).inflate(R.layout.sheet_identra_app_picker, null, false);
        dialog.setContentView(v);
        try { dialog.getBehavior().setSkipCollapsed(true); dialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED); } catch (Throwable ignored) { }
        ((TextView) v.findViewById(R.id.identraPickerTitle)).setText(title);
        final EditText search = v.findViewById(R.id.identraPickerSearch);
        final ChipGroup chips = v.findViewById(R.id.identraPickerChips);
        final TextView empty = v.findViewById(R.id.identraPickerEmpty);
        final View progress = v.findViewById(R.id.identraPickerProgress);
        RecyclerView rv = v.findViewById(R.id.identraPickerList);
        rv.setLayoutManager(new LinearLayoutManager(context));
        final IdentraListAdapter adapter = new IdentraListAdapter();
        rv.setAdapter(adapter);

        final Runnable[] render = new Runnable[1];
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
                items.add(IdentraListAdapter.Item.row(0, label, app.packageName)
                        .app(app.packageName, app.icon)
                        .trailing(prot ? context.getString(R.string.identra_detail_protected) : null)
                        .click(x -> { dialog.dismiss(); picked.onPicked(app); }));
            }
            for (IdentraListAdapter.Item i : items) i.chevron = false;
            adapter.submit(items);
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        };

        chips.setOnCheckedStateChangeListener((g, ids) -> render[0].run());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { render[0].run(); }
        });

        IdentraData.ensure(context, s -> {
            progress.setVisibility(View.GONE);
            boolean anyProtected = !IdentraData.protectedApps(s).isEmpty();
            chips.check(anyProtected ? R.id.identraPickerProtected : R.id.identraPickerUser);
            render[0].run();
        });
        dialog.show();
    }
}
