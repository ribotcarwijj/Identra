package dev.identra.app;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import java.util.Locale;

public class ApplicationEx extends Application {
    private static final String TAG = "Identra.App";

    @Override
    public void onCreate() {
        //setLocale();
        super.onCreate();
        //setLocale();
        Log.i(TAG, "Create version=" + BuildConfig.VERSION_NAME);
        // 0.6.1: framework service (hot reload) + periodic "Found in this app" scan
        dev.identra.app.reload.HotReloader.init();
        dev.identra.app.found.FoundScanJob.schedulePeriodic(getApplicationContext());
        Log.w(TAG, "SHA1 Fingerprint For [" + XCommandBridgeStatic.PRO_PACKAGE + "] package equals=[" + XSecurity.getProFingerPrint(getApplicationContext()) + "]");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //setLocale();
    }

    /*private void setLocale() {
        final Resources resources = getResources();
        final Configuration configuration = resources.getConfiguration();
        //final Locale locale = ActivityBase.getLocale(this);
        final Locale locale = new Locale("es");
        if (!configuration.locale.equals(locale)) {
            configuration.setLocale(locale);
            resources.updateConfiguration(configuration, null);
        }
    }*/
}