package dev.identra.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Trace;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import dev.identra.app.api.hook.LuaHooksGroup;
import dev.identra.app.api.hook.IdentraHook;

import dev.identra.app.api.xstandard.interfaces.IDividerKind;
import dev.identra.app.ui.GroupHelper;
import dev.identra.app.ui.HookWarnings;
import dev.identra.app.ui.dialogs.HookWarningDialog;
import dev.identra.app.ui.interfaces.ILoader;
import dev.identra.app.utilities.SettingUtil;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.utils.TryRun;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.ui.dialogs.HookInfoDialog;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.hook.AssignmentPacket;
import dev.identra.app.x.core.hook.AppXpPacket;


public class AdapterGroup extends RecyclerView.Adapter<AdapterGroup.ViewHolder> implements IDividerKind {
    private AppXpPacket app;
    private List<LuaHooksGroup> groups = new ArrayList<>();
    private ILoader fragmentLoader;
    private static final int VT_HEADER = 0;
    private static final int VT_GROUP = 1;

    public static interface IFinished {
        void onFinish();
    }

    @Override
    public String getDividerID(int position) { return position == 0 ? "__all__" : groups.get(position - 1).groupId; }

    @Override
    public String getLongID(int position) { return position == 0 ? "__all__" : groups.get(position - 1).name; }

    @Override
    public boolean isSearching() { return false; }

    @Override
    public boolean hasChanged() { return false; }

    @Override
    public void resetHashChanged() { }

    public class ViewHolder extends
            RecyclerView.ViewHolder
            implements CompoundButton.OnCheckedChangeListener,
            View.OnClickListener,
            View.OnLongClickListener {
        boolean header = false;
        final View itemView;
        final ImageView ivException;
        final ImageView ivInstalled;
        final ImageView ivInfo;
        final TextView tvUsed;
        final TextView tvGroup;
        final AppCompatCheckBox cbAssigned;

        ViewHolder(View itemView) {
            super(itemView);

            this.itemView = itemView;
            ivException = itemView.findViewById(R.id.ivException);
            ivInstalled = itemView.findViewById(R.id.ivInstalled);
            ivInfo = itemView.findViewById(R.id.ivInfo);
            tvUsed = itemView.findViewById(R.id.tvUsed);
            tvGroup = itemView.findViewById(R.id.tvGroup);
            cbAssigned = itemView.findViewById(R.id.cbAssigned);
        }

        private void wire() {
            ivInfo.setOnClickListener(this);
            ivException.setOnClickListener(this);
            tvGroup.setOnClickListener(this);
            cbAssigned.setOnCheckedChangeListener(this);
            tvGroup.setOnLongClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        private void unWire() {
            ivInfo.setOnClickListener(null);
            ivException.setOnClickListener(null);
            tvGroup.setOnClickListener(null);
            cbAssigned.setOnCheckedChangeListener(null);
            tvGroup.setOnLongClickListener(null);
            itemView.setOnLongClickListener(null);
        }

        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View view) {
            if(header) {
                if(view.getId() == R.id.tvGroup) cbAssigned.setChecked(!cbAssigned.isChecked());
                return;
            }
            int pos = getAdapterPosition() - 1;
            if(pos < 0 || pos >= groups.size()) return;
            LuaHooksGroup group = groups.get(pos);
            switch (view.getId()) {
                case R.id.ivInfo:
                    String name = group.getCleanTitle();
                    String msg = HookInfoDialog.getMessage(view.getContext(), name);
                    if(DebugUtil.isDebug())
                        Log.d(LibUtil.generateTag(AdapterGroup.class), "INFO CLICK, Name=" + name + " Msg=" + msg);

                    //Trying to call it here
                    if(!Str.isEmpty(msg))
                        HookInfoDialog.create()
                                .setHookGroupName(name)
                                .setHookGroupMessage(msg)
                                .show(fragmentLoader.getManager(), "hook_info");
                    break;
                case R.id.ivException:
                    StringBuilder sb = new StringBuilder();
                    for (AssignmentPacket assignment : app.getAssignments(group.name))
                        if (assignment.hookObj.group.equals(group.name))
                            if (assignment.exception != null) {
                                sb.append("<b>");
                                sb.append(Html.escapeHtml(assignment.hookObj.getObjectId()));
                                sb.append("</b><br><br>");
                                for (String line : assignment.exception.split("\n")) {
                                    sb.append(Html.escapeHtml(line));
                                    sb.append("<br>");
                                }
                                sb.append("<br><br>");
                            }

                    LayoutInflater inflater = LayoutInflater.from(view.getContext());
                    View alert = inflater.inflate(R.layout.exception, null, false);
                    TextView tvException = alert.findViewById(R.id.tvException);
                    tvException.setText(Html.fromHtml(sb.toString()));

                    new AlertDialog.Builder(view.getContext())
                            .setView(alert)
                            .create()
                            .show();
                    break;

                case R.id.tvGroup:
                    cbAssigned.setChecked(!cbAssigned.isChecked());//Invoke the onCheck
                    break;

                //if they click on the Name then pop up
            }
        }

        @SuppressLint("NonConstantResourceId")
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
            if(header) {
                // Master switch: all groups on / off
                if(app == null) return;
                ArrayList<String> ids = new ArrayList<>();
                java.util.HashSet<String> names = new java.util.HashSet<>();
                for (LuaHooksGroup g : groups) {
                    names.add(g.name);
                    for (XHook h : g.hooks) if(h.getObjectId() != null) ids.add(h.getObjectId());
                }
                if(!checked) {
                    for (AssignmentPacket a : new ArrayList<>(app.assignments)) {
                        if(a == null || a.hookObj == null) continue;
                        String id = a.getHookId();
                        if(id != null && names.contains(a.hookObj.group) && !ids.contains(id)) ids.add(id);
                    }
                }
                app.setHooksAssigned(compoundButton.getContext(), ids, checked);
                return;
            }
            int pos = getAdapterPosition() - 1;
            if(pos < 0 || pos >= groups.size()) return;
            final LuaHooksGroup group = groups.get(pos);
            switch (compoundButton.getId()) {
                case R.id.cbAssigned:
                    if(group.hasWarning && checked) {
                        String wMsg = HookWarnings.getWarningMessage(compoundButton.getContext(), group.name);
                        if(wMsg != null)
                            new HookWarningDialog()
                                    .setGroup(group)
                                    .setText(wMsg)
                                    .show(fragmentLoader.getManager(), compoundButton.getContext().getString(R.string.title_hook_warning));
                    }

                    app.setAssigned(compoundButton.getContext(), group.name, checked);
                    break;
            }
        }
        @Override
        public boolean onLongClick(View view) {
            if(header || app == null) return false;
            int pos = getAdapterPosition() - 1;
            if(pos < 0 || pos >= groups.size()) return false;
            showHooksDialog(view.getContext(), groups.get(pos));
            return true;
        }
    }

    private void showHooksDialog(Context context, LuaHooksGroup group) {
        final List<XHook> list = new ArrayList<>(group.hooks);
        if(list.isEmpty()) return;
        final java.util.HashSet<String> assignedIds = new java.util.HashSet<>();
        for (AssignmentPacket a : app.getAssignments(group.name)) {
            String id = a.getHookId();
            if(id != null) assignedIds.add(id.toLowerCase());
        }
        final String[] labels = new String[list.size()];
        final boolean[] before = new boolean[list.size()];
        final boolean[] now = new boolean[list.size()];
        for (int i = 0; i < list.size(); i++) {
            XHook h = list.get(i);
            String id = h.getObjectId();
            String n = h.getName();
            labels[i] = Str.isEmpty(n) ? String.valueOf(id) : n;
            before[i] = id != null && assignedIds.contains(id.toLowerCase());
            now[i] = before[i];
        }
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(group.getCleanTitle() + " (" + list.size() + ")")
                .setMultiChoiceItems(labels, now, (d, which, isChecked) -> now[which] = isChecked)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    ArrayList<String> on = new ArrayList<>();
                    ArrayList<String> off = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        if(now[i] == before[i]) continue;
                        if(now[i]) on.add(list.get(i).getObjectId());
                        else off.add(list.get(i).getObjectId());
                    }
                    if(!on.isEmpty()) app.setHooksAssigned(context, on, true);
                    if(!off.isEmpty()) app.setHooksAssigned(context, off, false);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton("All / None", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            boolean anyOff = false;
            for (boolean b : now) if(!b) { anyOff = true; break; }
            android.widget.ListView lv = dialog.getListView();
            for (int i = 0; i < now.length; i++) {
                now[i] = anyOff;
                lv.setItemChecked(i, anyOff);
            }
        }));
        dialog.show();
    }

    AdapterGroup() { setHasStableIds(true); }
    AdapterGroup(ILoader loader) { this(); this.fragmentLoader = loader; }

    @SuppressLint("NotifyDataSetChanged")
    void set(AppXpPacket app, List<XHook> hooks, Context context, IFinished onFinished) {
        this.app = app;
        TryRun.silent(() -> {
            Map<String, LuaHooksGroup> map = new HashMap<>();
            for (XHook hook : hooks) {
                if(!Str.isEmpty(hook.group) &&
                        !hook.group.toLowerCase().startsWith("intercept.") &&
                        (hook.enabled == null || Boolean.TRUE.equals(hook.enabled))) {
                    LuaHooksGroup group = map.get(hook.group);
                    if(group == null) {
                        group = new LuaHooksGroup();
                        map.put(hook.group, group);

                        Resources resources = context.getResources();
                        String name = hook.group.toLowerCase().replaceAll("[^a-z]", "_");
                        group.id = resources.getIdentifier("group_" + name, "string", context.getPackageName());
                        group.name = hook.group;
                        group.title = (group.id > 0 ? resources.getString(group.id) : hook.group);
                        group.groupId = GroupHelper.getGroupId(group.name);
                        group.hasWarning = HookWarnings.hasWarning(context, group.name);
                    }

                    group.hooks.add(hook);
                }
            }

            for (String groupId : map.keySet()) {
                for (AssignmentPacket assignment : app.assignments) {
                    if(assignment.hookObj != null && !Str.isEmpty(assignment.getHookId())) {
                        String groupName = assignment.hookObj.group;
                        if(!Str.isEmpty(groupName) &&
                                !groupName.toLowerCase().startsWith("intercept.") &&
                                (assignment.hookObj.enabled == null || Boolean.TRUE.equals(assignment.hookObj.enabled))) {
                            if (assignment.hookObj.group.equals(groupId)) {
                                LuaHooksGroup group = map.get(groupId);
                                if(group == null)
                                    continue;

                                if (assignment.exception != null)
                                    group.exception = true;
                                if (assignment.installed >= 0)
                                    group.installed++;
                                if(assignment.hookObj.optional)
                                    group.optional++;
                                if (assignment.restricted)
                                    group.used = Math.max(group.used, assignment.used);

                                group.assigned++;
                            }
                        }
                    }
                }
            }

            this.groups = new ArrayList<>(map.values());
            final Collator collator = Collator.getInstance(Locale.getDefault());
            collator.setStrength(Collator.SECONDARY); // Case insensitive, process accents etc
            Collections.sort(this.groups, (group1, group2) -> collator.compare(group1.groupId, group2.groupId));

            TryRun.onMain(() -> {
                notifyDataSetChanged();//Invoke to Update the UI
                //Invoke parent
                if(onFinished != null) {
                    onFinished.onFinish();
                }
            });
        });
    }

    @Override
    public long getItemId(int position) { return position == 0 ? Long.MIN_VALUE + 7 : (groups.get(position - 1).name == null ? position : groups.get(position - 1).name.hashCode()); }

    @Override
    public int getItemViewType(int position) { return position == 0 ? VT_HEADER : VT_GROUP; }

    @Override
    public int getItemCount() { return groups.isEmpty() ? 0 : groups.size() + 1; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) { return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.group, parent, false)); }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.unWire();
        Resources res0 = holder.itemView.getContext().getResources();
        if(position == 0) {
            holder.header = true;
            int total = 0, assignedTotal = 0;
            for (LuaHooksGroup g : groups) { total += g.hooks.size(); assignedTotal += g.assigned; }
            holder.ivException.setVisibility(View.GONE);
            holder.ivInstalled.setVisibility(View.GONE);
            holder.ivInfo.setVisibility(View.INVISIBLE);
            holder.tvUsed.setVisibility(View.VISIBLE);
            holder.tvUsed.setText(assignedTotal + "/" + total);
            holder.tvGroup.setText("All groups");
            holder.tvGroup.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.cbAssigned.setChecked(assignedTotal > 0);
            holder.cbAssigned.setButtonTintList(ColorStateList.valueOf(res0.getColor(
                    total > 0 && assignedTotal >= total ? R.color.colorAccent : android.R.color.darker_gray, null)));
            holder.wire();
            return;
        }
        holder.header = false;
        holder.ivInfo.setVisibility(View.VISIBLE);
        holder.tvGroup.setTypeface(null, android.graphics.Typeface.NORMAL);
        LuaHooksGroup group = groups.get(position - 1);

        Context context = holder.itemView.getContext();
        Resources resources = holder.itemView.getContext().getResources();

        holder.ivException.setVisibility(group.hasException() ? View.VISIBLE : View.GONE);
        holder.ivInstalled.setVisibility(group.hasInstalled() ? View.VISIBLE : View.GONE);
        holder.ivInstalled.setAlpha(group.allInstalled() ? 1.0f : 0.5f);
        holder.tvUsed.setVisibility(group.lastUsed() < 0 ? View.GONE : View.VISIBLE);
        holder.tvUsed.setText(DateUtils.formatDateTime(context, group.lastUsed(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL));


        holder.tvGroup.setText(group.getCleanTitle());
        holder.cbAssigned.setChecked(group.hasAssigned());
        holder.cbAssigned.setButtonTintList(ColorStateList.valueOf(resources.getColor(
                group.allAssigned() ? R.color.colorAccent : android.R.color.darker_gray, null)));

        holder.wire();
    }
}
