package dev.identra.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import dev.identra.app.R;

/** Builder for the flat Identra bottom sheets (details, editors, confirmations). */
public final class IdentraSheet {
    public static final int STYLE_FILLED = 0, STYLE_TONAL = 1, STYLE_TEXT = 2, STYLE_OUTLINED = 3;

    private final Context context;
    private final BottomSheetDialog dialog;
    private final View root;
    private final LinearLayout content, actions;

    public IdentraSheet(Context context) {
        this.context = context;
        this.dialog = new BottomSheetDialog(context);
        this.root = LayoutInflater.from(context).inflate(R.layout.sheet_identra_detail, null, false);
        this.content = root.findViewById(R.id.identraSheetContent);
        this.actions = root.findViewById(R.id.identraSheetActions);
        dialog.setContentView(root);
        try {
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            dialog.getBehavior().setSkipCollapsed(true);
        } catch (Throwable ignored) { }
    }

    public IdentraSheet title(CharSequence t) {
        ((TextView) root.findViewById(R.id.identraSheetTitle)).setText(t);
        return this;
    }

    public IdentraSheet sub(CharSequence s) {
        TextView tv = root.findViewById(R.id.identraSheetSub);
        tv.setText(s);
        tv.setVisibility(s == null || s.length() == 0 ? View.GONE : View.VISIBLE);
        return this;
    }

    public LinearLayout content() { return content; }

    /** Label + value pair; long-press copies the value. */
    public IdentraSheet field(CharSequence label, final CharSequence value) {
        if (value == null || value.length() == 0) return this;
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, IdentraRows.dp(context, 6), 0, IdentraRows.dp(context, 6));
        TextView l = new TextView(context);
        l.setTextAppearance(R.style.TextAppearance_Identra_Caption);
        l.setText(label);
        TextView v = new TextView(context);
        v.setTextAppearance(R.style.TextAppearance_Identra_RowTitle);
        v.setText(value);
        v.setTextIsSelectable(true);
        box.addView(l);
        box.addView(v);
        box.setOnLongClickListener(x -> { copy(context, label, value); return true; });
        content.addView(box);
        return this;
    }

    /** Monospace block (scripts, raw values) on a tonal background, horizontally scrollable. */
    public IdentraSheet mono(CharSequence label, CharSequence text) {
        if (text == null || text.length() == 0) return this;
        if (label != null) {
            TextView l = new TextView(context);
            l.setTextAppearance(R.style.TextAppearance_Identra_Caption);
            l.setText(label);
            l.setPadding(0, IdentraRows.dp(context, 10), 0, IdentraRows.dp(context, 6));
            content.addView(l);
        }
        HorizontalScrollView hs = new HorizontalScrollView(context);
        hs.setBackgroundResource(R.drawable.bg_identra_mono);
        int p = IdentraRows.dp(context, 12);
        hs.setPadding(p, p, p, p);
        TextView v = new TextView(context);
        v.setTextAppearance(R.style.TextAppearance_Identra_Mono);
        v.setText(text);
        v.setTextIsSelectable(true);
        hs.addView(v);
        content.addView(hs, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return this;
    }

    public IdentraSheet text(CharSequence text) {
        TextView v = new TextView(context);
        v.setTextAppearance(R.style.TextAppearance_Identra_RowSub);
        v.setText(text);
        v.setPadding(0, 0, 0, IdentraRows.dp(context, 8));
        content.addView(v);
        return this;
    }

    /** Outlined text field; returns the edit text. */
    public TextInputEditText input(CharSequence hint, CharSequence value, boolean multiLine) {
        TextInputLayout til = (TextInputLayout) LayoutInflater.from(context).inflate(R.layout.item_identra_text_field, content, false);
        til.setHint(hint);
        TextInputEditText et = til.findViewById(R.id.identraFieldEdit);
        if (multiLine) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            et.setMaxLines(8);
        } else {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            et.setMaxLines(1);
        }
        if (value != null) et.setText(value);
        content.addView(til);
        return et;
    }

    public MaterialButton action(int style, CharSequence text, final View.OnClickListener onClick, boolean dismiss) {
        int layout;
        switch (style) {
            case STYLE_TONAL: layout = R.layout.item_identra_button_tonal; break;
            case STYLE_TEXT: layout = R.layout.item_identra_button_text; break;
            case STYLE_OUTLINED: layout = R.layout.item_identra_button_outlined; break;
            default: layout = R.layout.item_identra_button_filled; break;
        }
        MaterialButton b = (MaterialButton) LayoutInflater.from(context).inflate(layout, actions, false);
        b.setText(text);
        b.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(v);
            if (dismiss) dialog.dismiss();
        });
        actions.addView(b);
        return b;
    }

    public BottomSheetDialog dialog() { return dialog; }

    public IdentraSheet show() {
        dialog.show();
        return this;
    }

    public void dismiss() { dialog.dismiss(); }

    public static void copy(Context context, CharSequence label, CharSequence value) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText(label, value));
            Toast.makeText(context, R.string.identra_copied, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) { }
    }
}
