package dev.identra.app.ui;

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
import dev.identra.app.x.ui.core.UserClientAppContext;

/** Small, generic recycler adapter for the rebuilt tool screens (headers + rows). */
public class IdentraListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0, TYPE_ROW = 1;

    public interface Toggle { void onToggle(Item item, boolean on); }

    public static class Item {
        public boolean header;
        public int icon;
        public String appPackage;
        public int appIcon;
        public CharSequence title, sub, trailing;
        public Boolean checked;      // null = no switch
        public boolean switchEnabled = true;
        public boolean chevron;
        public boolean rotateChevron;
        public int indentDp;
        public Object tag;
        public View.OnClickListener onClick;
        public View.OnLongClickListener onLongClick;
        public Toggle onToggle;

        public static Item header(CharSequence title) {
            Item i = new Item();
            i.header = true;
            i.title = title;
            return i;
        }

        public static Item row(int icon, CharSequence title, CharSequence sub) {
            Item i = new Item();
            i.icon = icon;
            i.title = title;
            i.sub = sub;
            return i;
        }

        public Item click(View.OnClickListener l) { this.onClick = l; this.chevron = l != null; return this; }
        public Item longClick(View.OnLongClickListener l) { this.onLongClick = l; return this; }
        public Item trailing(CharSequence t) { this.trailing = t; return this; }
        public Item toggle(boolean on, Toggle t) { this.checked = on; this.onToggle = t; this.chevron = false; return this; }
        public Item app(String pkg, int iconRes) { this.appPackage = pkg; this.appIcon = iconRes; return this; }
        public Item tag(Object t) { this.tag = t; return this; }
    }

    private final List<Item> items = new ArrayList<>();

    public void submit(List<Item> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public List<Item> items() { return items; }

    @Override public int getItemViewType(int position) { return items.get(position).header ? TYPE_HEADER : TYPE_ROW; }
    @Override public int getItemCount() { return items.size(); }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        View v = inf.inflate(viewType == TYPE_HEADER ? R.layout.item_identra_list_header : R.layout.item_identra_list_row, parent, false);
        return new RecyclerView.ViewHolder(v) { };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final Item item = items.get(position);
        View v = holder.itemView;
        if (item.header) {
            ((TextView) v).setText(item.title);
            return;
        }
        int start = IdentraRows.dp(v.getContext(), 20 + item.indentDp);
        v.setPaddingRelative(start, v.getPaddingTop(), v.getPaddingEnd(), v.getPaddingBottom());
        ImageView icon = v.findViewById(R.id.identraListIcon);
        if (item.appPackage != null) {
            icon.setVisibility(View.VISIBLE);
            icon.setBackground(null);
            icon.setPadding(0, 0, 0, 0);
            if (item.appIcon <= 0) icon.setImageResource(android.R.drawable.sym_def_app_icon);
            else UserClientAppContext.attachIcon(v.getContext(), IdentraRows.dp(v.getContext(), 36), icon, item.appPackage, item.appIcon);
        } else if (item.icon != 0) {
            int p = IdentraRows.dp(v.getContext(), 8);
            icon.setVisibility(View.VISIBLE);
            icon.setBackgroundResource(R.drawable.bg_identra_icon_circle);
            icon.setPadding(p, p, p, p);
            icon.setImageResource(item.icon);
        } else {
            icon.setVisibility(View.GONE);
        }
        ((TextView) v.findViewById(R.id.identraListTitle)).setText(item.title);
        TextView sub = v.findViewById(R.id.identraListSub);
        sub.setText(item.sub);
        sub.setVisibility(item.sub == null || item.sub.length() == 0 ? View.GONE : View.VISIBLE);
        TextView trailing = v.findViewById(R.id.identraListTrailing);
        trailing.setText(item.trailing);
        trailing.setVisibility(item.trailing == null || item.trailing.length() == 0 ? View.GONE : View.VISIBLE);

        final MaterialSwitch sw = v.findViewById(R.id.identraListSwitch);
        sw.setOnCheckedChangeListener(null);
        if (item.checked != null) {
            sw.setVisibility(View.VISIBLE);
            sw.setChecked(item.checked);
            sw.setEnabled(item.switchEnabled);
            sw.setOnCheckedChangeListener((b, on) -> {
                item.checked = on;
                if (item.onToggle != null) item.onToggle.onToggle(item, on);
            });
        } else {
            sw.setVisibility(View.GONE);
        }
        View chevron = v.findViewById(R.id.identraListChevron);
        chevron.setVisibility(item.chevron ? View.VISIBLE : View.GONE);
        chevron.setRotation(item.rotateChevron ? 90f : 0f);

        if (item.onClick != null) {
            v.setOnClickListener(item.onClick);
        } else if (item.checked != null) {
            v.setOnClickListener(x -> { if (sw.isEnabled()) sw.toggle(); });
        } else {
            v.setOnClickListener(null);
            v.setClickable(item.onLongClick != null);
        }
        v.setOnLongClickListener(item.onLongClick);
        if (item.onLongClick == null) v.setLongClickable(false);
    }
}
