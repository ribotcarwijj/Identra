package dev.identra.app.ui.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import dev.identra.app.ui.IdentraData;
import dev.identra.app.ui.profiles.AppDataVault;

/** 0.7.24: target of the "Add to home screen" shortcut: starts the app inside its hidden instance, then goes away. */
public class InstanceLaunchActivity extends Activity {
    static final String EXTRA_USER = "identra.user", EXTRA_PKG = "identra.pkg";

    static Intent intent(Context c, int user, String pkg) {
        Intent i = new Intent(c, InstanceLaunchActivity.class);
        i.setAction(Intent.ACTION_VIEW);
        i.putExtra(EXTRA_USER, user);
        i.putExtra(EXTRA_PKG, pkg);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return i;
    }

    static String script(int user, String pkg) {
        String q = "'" + pkg.replace("'", "") + "'";
        return "am start-user " + user + " >/dev/null 2>&1; C=$(cmd package resolve-activity --brief --user " + user
                + " -c android.intent.category.LAUNCHER " + q + " | tail -1); "
                + "case \"$C\" in */*) am start --user " + user + " -n \"$C\" >/dev/null 2>&1 && echo OK;; *) echo MISSING;; esac";
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final int user = getIntent().getIntExtra(EXTRA_USER, -1);
        final String pkg = getIntent().getStringExtra(EXTRA_PKG);
        final Context app = getApplicationContext();
        if (user > 0 && pkg != null) {
            IdentraData.EXECUTOR.submit(() -> {
                AppDataVault.Result r = AppDataVault.run(script(user, pkg), 30);
                if (!r.output.contains("OK"))
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            Toast.makeText(app, dev.identra.app.R.string.identra_instances_launch_failed, Toast.LENGTH_LONG).show());
            });
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
