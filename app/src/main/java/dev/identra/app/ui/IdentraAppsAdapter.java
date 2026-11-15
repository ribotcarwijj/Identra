package dev.identra.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

import dev.identra.app.R;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.ui.core.UserClientAppContext;

public class IdentraAppsAdapter extends RecyclerView.Adapter<IdentraAppsAdapter.Holder> {

    public interface Listener {
        void onOpen(AppXpPacket app);
        void onToggle(AppXpPacket app, boolean on, MaterialSwitch view);
    }

    private final Listener listener;
    private final int iconSize;
    private List<AppXpPacket> items = new ArrayList<>();
    private IdentraData.Snapshot snapshot;

    public IdentraAppsAdapter(Context context, Listener listener) {
        this.listener = listener;
        this.iconSize = IdentraRows.dp(context, 40);
        setHasStableIds(true);
    }

    public void submit(IdentraData.Snapshot snapshot, List<AppXpPacket> items) {
        this.snapshot = snapshot;
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        AppXpPacket a = items.get(position);
        return ((long) a.packageName.hashCode()) << 32 | (a.uid & 0xffffffffL);
    }

    @Override
    public int getItemCount() { return items.size(); }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_identra_app, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final AppXpPacket app = items.get(position);
        Context ctx = h.itemView.getContext();

        if (app.icon <= 0) h.icon.setImageResource(android.R.drawable.sym_def_app_icon);
        else UserClientAppContext.attachIcon(ctx, iconSize, h.icon, app.packageName, app.icon);

        h.name.setText(app.label == null || app.label.isEmpty() ? app.packageName : app.label);
        h.status.setText(statusText(ctx, snapshot, app));

        h.toggle.setOnCheckedChangeListener(null);
        h.toggle.setChecked(IdentraData.isProtected(app));
        h.toggle.setOnCheckedChangeListener((b, checked) -> listener.onToggle(app, checked, h.toggle));
        h.itemView.setOnClickListener(v -> listener.onOpen(app));
    }

    public static String statusText(Context ctx, IdentraData.Snapshot s, AppXpPacket app) {
        String base;
        if (!IdentraData.isProtected(app)) base = ctx.getString(R.string.identra_status_off);
        else {
            int n = IdentraData.assignedGroups(s, app).size();
            base = n == 1 ? ctx.getString(R.string.identra_status_protected_one) : ctx.getString(R.string.identra_status_protected, n);
        }
        dev.identra.app.ui.profiles.Profile p = dev.identra.app.ui.profiles.ProfileStore.get(ctx).profileFor(app.packageName, app.uid);
        return p == null ? base : base + " · " + ctx.getString(R.string.identra_status_profile, p.name);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name, status;
        final MaterialSwitch toggle;

        Holder(View v) {
            super(v);
            icon = v.findViewById(R.id.identraAppIcon);
            name = v.findViewById(R.id.identraAppName);
            status = v.findViewById(R.id.identraAppStatus);
            toggle = v.findViewById(R.id.identraAppSwitch);
        }
    }
}
