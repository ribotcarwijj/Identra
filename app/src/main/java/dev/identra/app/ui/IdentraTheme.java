package dev.identra.app.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

/** Night-mode + language handling for the new UI activities (local prefs only). */
public final class IdentraTheme {
    private IdentraTheme() { }

    public static final String PREF_FOLLOW_SYSTEM = "identra_ui_follow_system";
    public static final String PREF_DARK = "identra_ui_dark";
    /** Same pref key the classic ActivityBase uses. */
    public static final String PREF_FORCE_ENGLISH = "forceenglish";

    public static SharedPreferences prefs(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
    }

    public static boolean followsSystem(Context c) { return prefs(c).getBoolean(PREF_FOLLOW_SYSTEM, true); }
    public static boolean isDark(Context c) { return prefs(c).getBoolean(PREF_DARK, false); }

    public static int nightMode(Context c) {
        if (followsSystem(c)) return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        return isDark(c) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
    }

    /** Call before super.onCreate(). */
    @SuppressWarnings("deprecation")
    public static void apply(Activity activity) {
        int mode = nightMode(activity);
        if (AppCompatDelegate.getDefaultNightMode() != mode)
            AppCompatDelegate.setDefaultNightMode(mode);
        // 0.7.25: "Force English" is gone. Overwriting the base resources' configuration also overwrote the
        // night-mode bit AppCompat sets, which froze the app in light mode. Clear the old flag once.
        if (prefs(activity).contains(PREF_FORCE_ENGLISH)) prefs(activity).edit().remove(PREF_FORCE_ENGLISH).apply();
    }
}
