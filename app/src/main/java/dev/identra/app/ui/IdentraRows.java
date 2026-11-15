package dev.identra.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.materialswitch.MaterialSwitch;

import dev.identra.app.R;

/** Small builders for the flat card/row look of the new UI. */
public final class IdentraRows {
    private IdentraRows() { }

    public static void section(ViewGroup parent, CharSequence title) {
        TextView tv = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_identra_section, parent, false);
        tv.setText(title);
        parent.addView(tv);
    }

    /** A rounded card; rows are added to the returned container. */
    public static LinearLayout card(ViewGroup parent) {
        View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_identra_card_group, parent, false);
        parent.addView(card);
        return card.findViewById(R.id.identraCardRows);
    }

    public static View row(ViewGroup parent, int iconRes, CharSequence title, CharSequence sub, View.OnClickListener onClick) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_identra_row, parent, false);
        bindText(row, iconRes, title, sub);
        if (onClick != null) {
            row.findViewById(R.id.identraRowChevron).setVisibility(View.VISIBLE);
            row.setOnClickListener(onClick);
        } else {
            row.setClickable(false);
            row.setBackground(null);
        }
        parent.addView(row);
        return row;
    }

    public static MaterialSwitch switchRow(ViewGroup parent, int iconRes, CharSequence title, CharSequence sub,
                                           boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_identra_row, parent, false);
        bindText(row, iconRes, title, sub);
        final MaterialSwitch sw = row.findViewById(R.id.identraRowSwitch);
        sw.setVisibility(View.VISIBLE);
        // 0.7.28: every row's switch shares one view id. On an activity recreate (day/night change) Android
        // restored the saved checked state by id onto ALL switches and fired their listeners, which wrote
        // Dark=off / Follow system=off back to prefs, so the theme flipped straight back to light.
        // setSaveEnabled alone is not enough: restore still runs for a matching id, so give each switch its own
        // fresh id (nothing saved can match it) and keep its state out of the saved hierarchy.
        sw.setId(View.generateViewId());
        sw.setSaveEnabled(false);
        sw.setSaveFromParentEnabled(false);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(listener);
        row.setOnClickListener(v -> { if (sw.isEnabled()) sw.toggle(); });
        parent.addView(row);
        return sw;
    }

    public static void setSub(View row, CharSequence sub) {
        TextView tv = row.findViewById(R.id.identraRowSub);
        tv.setText(sub);
        tv.setVisibility(sub == null || sub.length() == 0 ? View.GONE : View.VISIBLE);
    }

    private static void bindText(View row, int iconRes, CharSequence title, CharSequence sub) {
        ImageView iv = row.findViewById(R.id.identraRowIcon);
        if (iconRes != 0) {
            iv.setImageResource(iconRes);
            iv.setVisibility(View.VISIBLE);
        }
        ((TextView) row.findViewById(R.id.identraRowTitle)).setText(title);
        setSub(row, sub);
    }

    public static int dp(Context c, int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }
}
