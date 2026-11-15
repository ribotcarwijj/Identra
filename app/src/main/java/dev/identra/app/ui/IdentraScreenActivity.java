package dev.identra.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import dev.identra.app.R;

/**
 * Shell for the rebuilt tool screens (0.3.0): collapsing large title, back arrow,
 * and either a scrolling card body or a searchable list body.
 */
public abstract class IdentraScreenActivity extends AppCompatActivity {
    protected final Handler main = new Handler(Looper.getMainLooper());

    protected MaterialToolbar toolbar;
    private CollapsingToolbarLayout collapsing;
    private FrameLayout body;
    private View progress;

    // list body
    protected RecyclerView recycler;
    protected EditText search;
    protected ChipGroup chips;
    private View empty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        IdentraTheme.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identra_screen);
        toolbar = findViewById(R.id.identraScreenToolbar);
        collapsing = findViewById(R.id.identraScreenCollapsing);
        body = findViewById(R.id.identraScreenBody);
        progress = findViewById(R.id.identraScreenProgress);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (collapsing != null) collapsing.setTitle(title);
    }

    public void setScreenTitle(int res) { setTitle(getString(res)); }

    /** Scrolling body; returns the vertical container to add cards to. */
    protected LinearLayout useScrollBody() {
        body.removeAllViews();
        View v = LayoutInflater.from(this).inflate(R.layout.include_identra_scroll, body, false);
        body.addView(v);
        return v.findViewById(R.id.identraScrollContent);
    }

    /** Searchable list body. */
    protected RecyclerView useListBody(int searchHint, final SearchListener listener) {
        body.removeAllViews();
        View v = LayoutInflater.from(this).inflate(R.layout.include_identra_list, body, false);
        body.addView(v);
        recycler = v.findViewById(R.id.identraListRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        search = v.findViewById(R.id.identraListSearch);
        chips = v.findViewById(R.id.identraListChips);
        empty = v.findViewById(R.id.identraListEmpty);
        if (searchHint != 0) {
            search.setHint(searchHint);
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(Editable s) {
                    if (listener != null) listener.onQuery(s == null ? "" : s.toString().trim());
                }
            });
        } else {
            v.findViewById(R.id.identraListSearchLayout).setVisibility(View.GONE);
        }
        return recycler;
    }

    public interface SearchListener { void onQuery(String query); }

    /** Adds a filter chip to the list body; the first added is checked. */
    protected Chip addChip(int id, int text) {
        View scroll = findViewById(R.id.identraListChipsScroll);
        if (scroll != null) scroll.setVisibility(View.VISIBLE);
        Chip c = (Chip) LayoutInflater.from(this).inflate(R.layout.item_identra_filter_chip, chips, false);
        c.setId(id);
        c.setText(text);
        chips.addView(c);
        if (chips.getChildCount() == 1) chips.check(id);
        return c;
    }

    protected void showEmpty(boolean show, int art, int title, int bodyText) {
        if (empty == null) return;
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;
        ((ImageView) empty.findViewById(R.id.identraEmptyArt)).setImageResource(art == 0 ? R.drawable.ill_identra_empty : art);
        ((TextView) empty.findViewById(R.id.identraEmptyTitle)).setText(title);
        TextView b = empty.findViewById(R.id.identraEmptyBody);
        if (bodyText != 0) { b.setText(bodyText); b.setVisibility(View.VISIBLE); }
        else b.setVisibility(View.GONE);
    }

    protected void setBusy(boolean busy) {
        if (progress != null) progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    protected void snack(CharSequence text) {
        Snackbar.make(findViewById(R.id.identraScreenRoot), text, Snackbar.LENGTH_LONG).show();
    }

    protected Context ctx() { return this; }

    protected static void fade(ViewGroup v) {
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(180).start();
    }
}
