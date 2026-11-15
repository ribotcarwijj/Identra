package dev.identra.app.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.4.0: the phone's REAL current location for the Location tab's "My location" button.
 *
 * Why this exists: in 0.3.0 the my-location button only re-centred the map on the selected
 * place or on the global SPOOFED location (location.latitude/longitude), so it showed whatever
 * point had been set earlier (e.g. a place in Germany) instead of where the phone is. Identra
 * also never declared or requested a location permission, so it could not read a real fix.
 *
 * Strategy: runtime permission (fine or coarse), then a fresh fix with
 * LocationManager.getCurrentLocation (API 30+) on every enabled provider (fused, gps, network)
 * at once, first non-null wins; requestLocationUpdates for older APIs. A last-known fix is used
 * immediately only when it is younger than {@link #FRESH_MS}; after a timeout a last-known fix
 * younger than {@link #STALE_MS} is returned and marked as old. Nothing is ever invented: no fix
 * means {@link Callback#onError}.
 *
 * Identra's own process is not in the module's scope, so these reads are not spoofed; the
 * fragment still warns when the user assigned Spoof.Location hooks to Identra itself.
 */
public final class IdentraRealLocation {
    private static final String TAG = "Identra.RealLocation";

    public static final long FRESH_MS = 2 * 60 * 1000L;
    public static final long STALE_MS = 15 * 60 * 1000L;
    public static final long TIMEOUT_MS = 25 * 1000L;

    public static final int ERROR_PERMISSION = 1, ERROR_DISABLED = 2, ERROR_NO_FIX = 3;

    public interface Callback {
        /** @param ageMs how old the fix is; @param stale true when it is an older last-known fix after a timeout */
        void onLocation(Location location, long ageMs, boolean stale);
        void onError(int error);
    }

    private IdentraRealLocation() { }

    public static boolean hasPermission(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static long ageMs(Location l) {
        if (l == null) return Long.MAX_VALUE;
        long nanos = l.getElapsedRealtimeNanos();
        if (nanos > 0) return Math.max(0, SystemClock.elapsedRealtime() - nanos / 1_000_000L);
        return Math.max(0, System.currentTimeMillis() - l.getTime());
    }

    public static boolean isMock(Location l) {
        if (l == null) return false;
        try {
            if (Build.VERSION.SDK_INT >= 31) return l.isMock();
            //noinspection deprecation
            return l.isFromMockProvider();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Handle to cancel a pending request (fragment destroyed). */
    public static final class Request {
        private final List<Runnable> cancels = new ArrayList<>();
        private boolean done;

        public synchronized void cancel() {
            done = true;
            for (Runnable r : cancels) { try { r.run(); } catch (Throwable ignored) { } }
            cancels.clear();
        }

        synchronized boolean finish() {
            if (done) return false;
            done = true;
            for (Runnable r : cancels) { try { r.run(); } catch (Throwable ignored) { } }
            cancels.clear();
            return true;
        }

        synchronized void onCancel(Runnable r) { if (!done) cancels.add(r); }
    }

    /** Callbacks arrive on the main thread. */
    @SuppressLint("MissingPermission")
    public static Request fetch(Context context, final Callback cb) {
        final Request req = new Request();
        final Handler main = new Handler(Looper.getMainLooper());
        final Context app = context.getApplicationContext();
        if (!hasPermission(app)) { main.post(() -> { if (req.finish()) cb.onError(ERROR_PERMISSION); }); return req; }

        final LocationManager lm = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) { main.post(() -> { if (req.finish()) cb.onError(ERROR_NO_FIX); }); return req; }

        List<String> providers = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 31) addIfEnabled(lm, providers, LocationManager.FUSED_PROVIDER);
        addIfEnabled(lm, providers, LocationManager.GPS_PROVIDER);
        addIfEnabled(lm, providers, LocationManager.NETWORK_PROVIDER);
        boolean locationOn = true;
        try { if (Build.VERSION.SDK_INT >= 28) locationOn = lm.isLocationEnabled(); } catch (Throwable ignored) { }
        if (providers.isEmpty() || !locationOn) {
            main.post(() -> { if (req.finish()) cb.onError(ERROR_DISABLED); });
            return req;
        }

        // newest last-known fix across all providers (not only enabled ones)
        Location best = null;
        try {
            for (String p : lm.getAllProviders()) {
                Location l = null;
                try { l = lm.getLastKnownLocation(p); } catch (Throwable ignored) { }
                if (l != null && (best == null || ageMs(l) < ageMs(best))) best = l;
            }
        } catch (Throwable t) {
            Log.w(TAG, "last known failed: " + t);
        }
        final Location lastKnown = best;
        if (lastKnown != null && ageMs(lastKnown) <= FRESH_MS) {
            main.post(() -> { if (req.finish()) cb.onLocation(lastKnown, ageMs(lastKnown), false); });
            return req;
        }

        final Runnable timeout = () -> {
            if (!req.finish()) return;
            if (lastKnown != null && ageMs(lastKnown) <= STALE_MS) cb.onLocation(lastKnown, ageMs(lastKnown), true);
            else cb.onError(ERROR_NO_FIX);
        };
        main.postDelayed(timeout, TIMEOUT_MS);
        req.onCancel(() -> main.removeCallbacks(timeout));

        for (final String provider : providers) {
            try {
                if (Build.VERSION.SDK_INT >= 30) {
                    final CancellationSignal signal = new CancellationSignal();
                    req.onCancel(signal::cancel);
                    lm.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(app), l -> {
                        if (l == null) return;   // this provider had nothing; others or the timeout decide
                        if (req.finish()) cb.onLocation(l, ageMs(l), false);
                    });
                } else {
                    final LocationListener listener = new LocationListener() {
                        @Override public void onLocationChanged(Location l) {
                            if (l != null && req.finish()) cb.onLocation(l, ageMs(l), false);
                        }
                        @Override public void onStatusChanged(String p, int s, Bundle b) { }
                        @Override public void onProviderEnabled(String p) { }
                        @Override public void onProviderDisabled(String p) { }
                    };
                    req.onCancel(() -> { try { lm.removeUpdates(listener); } catch (Throwable ignored) { } });
                    lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper());
                }
            } catch (Throwable t) {
                Log.w(TAG, "provider " + provider + " failed: " + t);
            }
        }
        return req;
    }

    private static void addIfEnabled(LocationManager lm, List<String> out, String provider) {
        try {
            if (lm.getAllProviders().contains(provider) && lm.isProviderEnabled(provider)) out.add(provider);
        } catch (Throwable ignored) { }
    }
}
