package dev.identra.app.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.identra.app.BuildConfig;
import dev.identra.app.R;

/**
 * Auto-update from GitHub Releases of the public Identra repo.
 * A release must carry an asset named like "Identra-v<versionName>-<versionCode>[-debug].apk"
 * (the Gradle archivesBaseName). Newer versionCode -> offer update -> download -> root `pm install -r`.
 */
public final class IdentraUpdater {
    private static final String TAG = "Identra.Update";
    public static final String REPO = "ribotcarwijj/Identra";
    private static final String API = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String PREFS = "identra_update";
    private static final String KEY_LAST = "last_check";
    private static final String KEY_AUTO = "auto_check";
    private static final String KEY_SKIP = "skip_code";
    private static final long INTERVAL_MS = 6L * 60 * 60 * 1000;
    private static final Pattern CODE = Pattern.compile("-(\\d+)(?:-[A-Za-z]+)?\\.apk$");

    private static volatile boolean running = false;

    public static final class Release {
        public String tag, name, notes, apkUrl, apkName, htmlUrl;
        public int versionCode;
    }

    private IdentraUpdater() {}

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public static boolean isAutoEnabled(Context c) { return prefs(c).getBoolean(KEY_AUTO, true); }
    public static void setAutoEnabled(Context c, boolean on) { prefs(c).edit().putBoolean(KEY_AUTO, on).apply(); }

    /** Called from ActivityMain.onCreate: silent, at most every 6h, only prompts when something newer exists. */
    public static void autoCheck(final Activity a) {
        if (!isAutoEnabled(a)) return;
        long last = prefs(a).getLong(KEY_LAST, 0);
        if (System.currentTimeMillis() - last < INTERVAL_MS) return;
        check(a, false);
    }

    /** Manual check from the drawer: always reports a result. */
    public static void check(final Activity a, final boolean manual) {
        if (running) return;
        running = true;
        if (manual) toast(a, a.getString(R.string.update_checking));
        new Thread(() -> {
            Release r = null; String err = null;
            try { r = fetchLatest(); } catch (Throwable t) { err = t.getMessage(); Log.w(TAG, "check failed: " + t); }
            prefs(a).edit().putLong(KEY_LAST, System.currentTimeMillis()).apply();
            running = false;
            final Release rel = r; final String e = err;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (a.isFinishing()) return;
                if (rel == null) { if (manual) toast(a, a.getString(R.string.update_failed, String.valueOf(e))); return; }
                if (rel.versionCode <= BuildConfig.VERSION_CODE) { if (manual) toast(a, a.getString(R.string.update_latest, BuildConfig.VERSION_NAME)); return; }
                if (!manual && prefs(a).getInt(KEY_SKIP, 0) == rel.versionCode) return;
                prompt(a, rel);
            });
        }, "IdentraUpdateCheck").start();
    }

    static Release fetchLatest() throws Exception {
        JSONObject j = new JSONObject(get(API));
        Release r = new Release();
        r.tag = j.optString("tag_name");
        r.name = j.optString("name", r.tag);
        r.notes = j.optString("body", "");
        r.htmlUrl = j.optString("html_url");
        JSONArray assets = j.optJSONArray("assets");
        if (assets != null) for (int i = 0; i < assets.length(); i++) {
            JSONObject as = assets.getJSONObject(i);
            String n = as.optString("name");
            Matcher m = CODE.matcher(n);
            if (n.endsWith(".apk") && m.find()) {
                int code = Integer.parseInt(m.group(1));
                if (code > r.versionCode) { r.versionCode = code; r.apkName = n; r.apkUrl = as.optString("browser_download_url"); }
            }
        }
        if (r.apkUrl == null) throw new IllegalStateException("no APK asset in " + r.tag);
        return r;
    }

    private static void prompt(final Activity a, final Release r) {
        String notes = r.notes == null ? "" : r.notes.trim();
        if (notes.length() > 1500) notes = notes.substring(0, 1500) + "…";
        new AlertDialog.Builder(a)
                .setTitle(a.getString(R.string.update_available, r.name))
                .setMessage(a.getString(R.string.update_current, BuildConfig.VERSION_NAME) + "\n\n" + notes)
                .setPositiveButton(R.string.update_install, (d, w) -> install(a, r))
                .setNeutralButton(R.string.update_skip, (d, w) -> prefs(a).edit().putInt(KEY_SKIP, r.versionCode).apply())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void install(final Activity a, final Release r) {
        toast(a, a.getString(R.string.update_downloading));
        new Thread(() -> {
            String err;
            try {
                File dir = new File(a.getCacheDir(), "update");
                dir.mkdirs();
                File apk = new File(dir, "identra-update.apk");
                download(r.apkUrl, apk);
                apk.setReadable(true, false);
                dir.setExecutable(true, false);
                // Root install (Identra already requires root). The app process is killed on success.
                err = suInstall(apk);
            } catch (Throwable t) { err = String.valueOf(t.getMessage()); }
            final String e = err;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (e != null && !a.isFinishing()) toast(a, a.getString(R.string.update_failed, e));
            });
        }, "IdentraUpdateInstall").start();
    }

    private static String suInstall(File apk) throws Exception {
        String tmp = "/data/local/tmp/identra-update.apk";
        Process p = new ProcessBuilder("su", "-c",
                "cp '" + apk.getAbsolutePath() + "' " + tmp + " && chmod 644 " + tmp +
                " && pm install -r -d " + tmp + "; rc=$?; rm -f " + tmp + "; exit $rc")
                .redirectErrorStream(true).start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String l; while ((l = br.readLine()) != null) out.append(l).append('\n');
        }
        int rc = p.waitFor();
        Log.i(TAG, "pm install rc=" + rc + " " + out);
        return (rc == 0 && out.toString().contains("Success")) ? null : out.toString().trim();
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("User-Agent", "Identra/" + BuildConfig.VERSION_NAME);
        int code = c.getResponseCode();
        if (code / 100 != 2) throw new IllegalStateException("HTTP " + code);
        return c;
    }

    private static String get(String url) throws Exception {
        HttpURLConnection c = open(url);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
            StringBuilder sb = new StringBuilder(); String l;
            while ((l = br.readLine()) != null) sb.append(l);
            return sb.toString();
        } finally { c.disconnect(); }
    }

    private static void download(String url, File to) throws Exception {
        HttpURLConnection c = open(url);
        try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[64 * 1024]; int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } finally { c.disconnect(); }
    }

    private static void toast(Context c, String s) { Toast.makeText(c, s, Toast.LENGTH_LONG).show(); }
}
