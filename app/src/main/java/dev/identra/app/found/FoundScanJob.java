package dev.identra.app.found;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.util.Log;

import dev.identra.app.ui.IdentraData;
import dev.identra.app.x.core.hook.AppXpPacket;

/**
 * 0.6.0: background scan of one app's dex after it was installed or updated (scheduled by
 * {@link FoundScanReceiver}, which system_server's package listener calls). Results are cached by
 * {@link FoundStore}; if "Use identity for everything found" is on for the app, new finds are
 * assigned right away.
 */
public class FoundScanJob extends JobService {
    private static final String TAG = "Identra.FoundJob";
    private static final String EXTRA_PKG = "pkg";
    private static final String EXTRA_UID = "uid";
    private static final int JOB_BASE = 0x1D7A0000;
    private static final int JOB_PERIODIC = 0x1D79FFFF;
    private static final String EXTRA_PERIODIC = "periodic";

    /**
     * 0.6.1: every 6 h (and works with any system part): scan apps installed/updated since the last
     * run (PackageManager.getChangedPackages) and add new finds for apps with the identity switch on.
     */
    public static void schedulePeriodic(Context context) {
        try {
            JobScheduler js = context.getSystemService(JobScheduler.class);
            if (js == null || js.getPendingJob(JOB_PERIODIC) != null) return;
            PersistableBundle b = new PersistableBundle();
            b.putInt(EXTRA_PERIODIC, 1);
            js.schedule(new JobInfo.Builder(JOB_PERIODIC, new ComponentName(context, FoundScanJob.class))
                    .setExtras(b)
                    .setPeriodic(6 * 60 * 60 * 1000L)
                    .setRequiresBatteryNotLow(true)
                    .build());
        } catch (Throwable t) {
            Log.w(TAG, "periodic: " + t);
        }
    }

    private static void runPeriodic(Context ctx) {
        android.content.SharedPreferences p = ctx.getSharedPreferences("identra_found_job", MODE_PRIVATE);
        int seq = p.getInt("seq", 0);
        android.content.pm.ChangedPackages ch = ctx.getPackageManager().getChangedPackages(seq);
        if (ch != null) {
            for (String pkg : ch.getPackageNames()) {
                if (pkg.startsWith(ctx.getPackageName())) continue;
                try { FoundStore.scan(ctx, pkg, false); } catch (Throwable ignored) { }
            }
            p.edit().putInt("seq", ch.getSequenceNumber()).apply();
        }
        java.util.Set<String> on = FoundStore.identityOnKeys(ctx);
        if (on.isEmpty()) return;
        IdentraData.Snapshot s = IdentraData.load(ctx);
        for (String key : on) {
            int c = key.lastIndexOf(':');
            if (c <= 0) continue;
            try {
                AppXpPacket app = IdentraData.findApp(s, key.substring(0, c), Integer.parseInt(key.substring(c + 1)));
                if (app != null) FoundIdentity.sync(ctx, app);
            } catch (Throwable ignored) { }
        }
    }

    public static void schedule(Context context, String pkg, int uid) {
        try {
            PersistableBundle b = new PersistableBundle();
            b.putString(EXTRA_PKG, pkg);
            b.putInt(EXTRA_UID, uid);
            JobInfo job = new JobInfo.Builder(JOB_BASE + (pkg.hashCode() & 0xFFFF), new ComponentName(context, FoundScanJob.class))
                    .setExtras(b)
                    .setOverrideDeadline(60_000L)
                    .build();
            JobScheduler js = context.getSystemService(JobScheduler.class);
            if (js != null) js.schedule(job);
        } catch (Throwable t) {
            Log.w(TAG, "schedule " + pkg + ": " + t);
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        if (params.getExtras().getInt(EXTRA_PERIODIC, 0) == 1) {
            new Thread(() -> {
                try { runPeriodic(getApplicationContext()); }
                catch (Throwable t) { Log.w(TAG, "periodic run: " + t); }
                finally { jobFinished(params, false); }
            }, "identra-found-periodic").start();
            return true;
        }
        final String pkg = params.getExtras().getString(EXTRA_PKG);
        final int uid = params.getExtras().getInt(EXTRA_UID, -1);
        if (pkg == null) return false;
        new Thread(() -> {
            try {
                FoundStore.scan(getApplicationContext(), pkg, false);
                if (FoundStore.isIdentityOn(getApplicationContext(), pkg, uid)) {
                    IdentraData.Snapshot s = IdentraData.load(getApplicationContext());
                    AppXpPacket app = IdentraData.findApp(s, pkg, uid);
                    if (app != null) FoundIdentity.sync(getApplicationContext(), app);
                }
            } catch (Throwable t) {
                Log.w(TAG, "job " + pkg + ": " + t);
            } finally {
                jobFinished(params, false);
            }
        }, "identra-found-scan").start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) { return false; }
}
