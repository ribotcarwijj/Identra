package dev.identra.app.found;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 0.6.0: system_server's package listener sends this explicit broadcast when an app is installed
 * or updated (implicit PACKAGE_ADDED no longer reaches manifest receivers). Not exported, so only
 * system / root can send it. Manual use: am broadcast -n dev.identra.app/.found.FoundScanReceiver
 * --es pkg com.example --ei uid 10123
 */
public class FoundScanReceiver extends BroadcastReceiver {
    public static final String EXTRA_PKG = "pkg";
    public static final String EXTRA_UID = "uid";

    @Override
    public void onReceive(Context context, Intent intent) {
        String pkg = intent.getStringExtra(EXTRA_PKG);
        int uid = intent.getIntExtra(EXTRA_UID, -1);
        if (pkg == null || pkg.startsWith(context.getPackageName())) return;
        FoundScanJob.schedule(context, pkg, uid);
    }
}
