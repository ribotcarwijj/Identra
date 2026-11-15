package dev.identra.app.ui.tools;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import dev.identra.app.BuildConfig;
import dev.identra.app.R;
import dev.identra.app.ui.IdentraRows;
import dev.identra.app.ui.IdentraScreenActivity;

/**
 * Help (0.5.0): short, expandable sections written for Identra (source: the user guide in the
 * ops docs). Replaces the old HTML help screen.
 */
public class IdentraHelpActivity extends IdentraScreenActivity {

    private static final int[][] SECTIONS = {
            {R.drawable.ic_identra_mark, R.string.identra_help_what_title, R.string.identra_help_what_body},
            {R.drawable.ic_identra_check, R.string.identra_help_start_title, R.string.identra_help_start_body},
            {R.drawable.ic_identra_shield, R.string.identra_help_protect_title, R.string.identra_help_protect_body},
            {R.drawable.ic_identra_person, R.string.identra_help_profiles_title, R.string.identra_help_profiles_body},
            {R.drawable.ic_identra_location, R.string.identra_help_location_title, R.string.identra_help_location_body},
            {R.drawable.ic_identra_code, R.string.identra_help_advanced_title, R.string.identra_help_advanced_body},
            {R.drawable.ic_identra_bug, R.string.identra_help_trouble_title, R.string.identra_help_trouble_body},
            {R.drawable.ic_identra_help, R.string.identra_help_faq_title, R.string.identra_help_faq_body},
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreenTitle(R.string.identra_about_help);
        LinearLayout root = useScrollBody();

        TextView intro = new TextView(this);
        intro.setTextAppearance(R.style.TextAppearance_Identra_RowSub);
        intro.setText(getString(R.string.identra_help_intro, BuildConfig.VERSION_NAME));
        int p = IdentraRows.dp(this, 4);
        intro.setPadding(p, p, p, IdentraRows.dp(this, 12));
        root.addView(intro);

        for (int i = 0; i < SECTIONS.length; i++) addSection(root, SECTIONS[i][0], SECTIONS[i][1], SECTIONS[i][2], i == 0);
    }

    private void addSection(LinearLayout root, int icon, int title, int body, boolean open) {
        LinearLayout card = IdentraRows.card(root);
        final View head = IdentraRows.row(card, icon, getString(title), null, v -> { });
        final TextView text = new TextView(this);
        text.setTextAppearance(R.style.TextAppearance_Identra_RowSub);
        text.setText(body);
        text.setTextIsSelectable(true);
        text.setLineSpacing(0f, 1.15f);
        int h = IdentraRows.dp(this, 20);
        text.setPadding(h, 0, h, IdentraRows.dp(this, 16));
        text.setVisibility(open ? View.VISIBLE : View.GONE);
        card.addView(text);
        final View chevron = head.findViewById(R.id.identraRowChevron);
        chevron.setRotation(open ? 90f : 0f);
        head.setOnClickListener(v -> {
            boolean show = text.getVisibility() != View.VISIBLE;
            text.setVisibility(show ? View.VISIBLE : View.GONE);
            chevron.animate().rotation(show ? 90f : 0f).setDuration(150).start();
        });
    }
}
