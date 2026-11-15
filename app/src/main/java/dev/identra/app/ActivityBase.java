package dev.identra.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import dev.identra.app.api.XResult;
import dev.identra.app.api.core.IdentraCall;
import dev.identra.app.utilities.PrefUtil;
import dev.identra.app.x.core.commands.call.GetSettingExCommand;

public class ActivityBase extends AppCompatActivity {
    private static final String TAG = "Identra.ActivityBase";
    private String theme;
    private boolean isForceEnglish;
    //private Locale mCurrentLocale;

    @Override
    protected void onStart() {
        super.onStart();
        //PrefUtil.setString(this, "language", "es");
        //mCurrentLocale = getResources().getConfiguration().locale;
        //mCurrentLocale = new Locale("es");
    }

    public void setForceEnglish(boolean force) {
        try {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            prefs.edit().putBoolean("forceenglish", force).apply();
        }catch (Exception ignored) { }
    }

    public boolean getIsForceEnglish() {
        try {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if(!prefs.contains("forceenglish")) {
                prefs.edit().putBoolean("forceenglish", false).apply();
                return false;
            }

            return prefs.getBoolean("forceenglish", false);
        }catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        theme = GetSettingExCommand.getTheme(this, Process.myUid());
        if(DebugUtil.isDebug())
            Log.d(TAG, "OnCreate Theme=" + theme);

        isForceEnglish = false;   // 0.7.25: option removed (it broke day/night)
        if (useIdentraTheme()) {
            // 0.3.0: classic tool screens get the Identra look (day/night follows the new UI setting)
            dev.identra.app.ui.IdentraTheme.apply(this);
            setTheme(R.style.Theme_Identra_Classic);
        } else {
            setTheme("dark".equals(theme) ? R.style.AppThemeDark : R.style.AppThemeLight);
        }
        if(isForceEnglish) {
            try {
                String languageToLoad  = "en"; // your language
                Locale locale = new Locale(languageToLoad);
                Locale.setDefault(locale);
                Configuration config = new Configuration();
                config.locale = locale;
                getBaseContext().getResources().updateConfiguration(config,
                        getBaseContext().getResources().getDisplayMetrics());
            }catch (Exception ignored) {

            }
        }

        //Both methods work tho the first one not sure how to change and keeps ovveriding to english this one one alone works as well
        /*String languageToLoad  = "es"; // your language
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());*/

        super.onCreate(savedInstanceState);
    }


    /*@Override
    protected void onRestart() {
        //set theme ?
        super.onRestart();
        Locale locale = getLocale(this);
        if (!locale.equals(mCurrentLocale)) {
            mCurrentLocale = locale;
            recreate();
        }
    }*/

    public String getThemeName() { return (theme == null ? "dark" : theme); }

    /** Classic screens use the Identra theme; the classic main screen keeps its original look. */
    protected boolean useIdentraTheme() { return true; }

    /*public static Locale getLocale(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String lang = sharedPreferences.getString("language", "es");
        switch (lang) {
            case "English":
                lang = "en";
                break;
            case "Spanish":
                lang = "es";
                break;
        }
        return new Locale(lang);
    }*/
}
