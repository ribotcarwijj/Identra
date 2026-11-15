package dev.identra.app.ui;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import java.util.Locale;

import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.call.GetSettingExCommand;
import dev.identra.app.x.core.commands.call.PutSettingExCommand;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.ActionFlag;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.settings.data.SettingPacket;

/**
 * Spoofed location values, stored with the SAME setting keys and the SAME put-setting
 * command the classic value editor uses (putExSettingEx):
 *   location.type      "set" (exact point) or "coarse" (blur the real location by location.accuracy metres)
 *   location.latitude  decimal degrees
 *   location.longitude decimal degrees
 *   location.accuracy  metres (used by the "coarse" mode of the Spoof.Location hooks)
 * Global = user of this app + the "global" namespace (PutSettingExCommand.putGen).
 * Per app = UserIdentity.fromUid(uid, packageName); per-app values override global ones
 * (PackageHookContext merges local over global). All calls block: run off the main thread.
 */
public final class IdentraLocation {
    private static final String TAG = "Identra.UiLocation";

    public static final String KEY_TYPE = "location.type";
    public static final String KEY_LAT = "location.latitude";
    public static final String KEY_LON = "location.longitude";
    public static final String KEY_ACCURACY = "location.accuracy";
    public static final String TYPE_SET = "set";
    public static final String TYPE_COARSE = "coarse";
    /** Hook group that reads these values; it must be on for an app to see the location. */
    public static final String GROUP = "Spoof.Location";

    private IdentraLocation() { }

    public static final class Value {
        public double lat, lon;
        public String type;
        public String accuracy;
        public boolean set;   // lat/lon present

        public String coords() { return format(lat) + ", " + format(lon); }
    }

    public static String format(double d) { return String.format(Locale.US, "%.6f", d); }

    public static A_CODE setGlobal(Context context, double lat, double lon, String type, String accuracy) {
        UserIdentity id = UserIdentity.fromUid(Process.myUid(), UserIdentity.GLOBAL_NAMESPACE);
        return write(context, id, lat, lon, type, accuracy, false);
    }

    public static A_CODE setForApp(Context context, int uid, String pkg, double lat, double lon, String type, String accuracy, boolean kill) {
        if (Str.isEmpty(pkg) || UserIdentity.GLOBAL_NAMESPACE.equalsIgnoreCase(pkg)) return A_CODE.FAILED;
        return write(context, UserIdentity.fromUid(uid, pkg), lat, lon, type, accuracy, kill);
    }

    /** Removes the per-app override so the app falls back to the global location. */
    public static A_CODE clearForApp(Context context, int uid, String pkg, boolean kill) {
        if (Str.isEmpty(pkg) || UserIdentity.GLOBAL_NAMESPACE.equalsIgnoreCase(pkg)) return A_CODE.FAILED;
        // Deleting a key that was never set may report a failure; one success is enough.
        A_CODE last = A_CODE.FAILED;
        boolean any = false;
        String[] keys = {KEY_ACCURACY, KEY_TYPE, KEY_LAT, KEY_LON};
        for (int i = 0; i < keys.length; i++) {
            boolean k = kill && i == keys.length - 1;
            SettingPacket p = new SettingPacket(keys[i], null, ActionPacket.create(ActionFlag.DELETE, k), UserIdentity.fromUid(uid, pkg));
            A_CODE c = PutSettingExCommand.call(context, p);
            if (A_CODE.isSuccessful(c)) any = true;
            else last = c;
        }
        return any ? A_CODE.SUCCESS : last;
    }

    private static A_CODE write(Context context, UserIdentity id, double lat, double lon, String type, String accuracy, boolean kill) {
        A_CODE result = A_CODE.SUCCESS;
        result = worst(result, put(context, id, KEY_TYPE, Str.isEmpty(type) ? TYPE_SET : type, false));
        result = worst(result, put(context, id, KEY_LAT, format(lat), false));
        boolean hasAcc = !Str.isEmpty(accuracy);
        result = worst(result, put(context, id, KEY_LON, format(lon), kill && !hasAcc));
        if (hasAcc) result = worst(result, put(context, id, KEY_ACCURACY, accuracy.trim(), kill));
        Log.i(TAG, "Location written cat=" + id.getCategory() + " result=" + result);
        return result;
    }

    private static A_CODE put(Context context, UserIdentity id, String key, String value, boolean kill) {
        SettingPacket p = new SettingPacket(key, value, ActionPacket.create(ActionFlag.PUSH, kill), id);
        return PutSettingExCommand.call(context, p);
    }

    private static A_CODE worst(A_CODE a, A_CODE b) { return A_CODE.isSuccessful(a) ? b : a; }

    public static Value readGlobal(Context context) {
        return read(context, Process.myUid(), UserIdentity.GLOBAL_NAMESPACE);
    }

    /** Values saved for this app only (null when the app has no override). */
    public static Value readForApp(Context context, int uid, String pkg) {
        Value v = read(context, uid, pkg);
        return v.set ? v : null;
    }

    private static Value read(Context context, int uid, String category) {
        Value v = new Value();
        try {
            String lat = value(GetSettingExCommand.get(context, KEY_LAT, uid, category));
            String lon = value(GetSettingExCommand.get(context, KEY_LON, uid, category));
            v.type = value(GetSettingExCommand.get(context, KEY_TYPE, uid, category));
            v.accuracy = value(GetSettingExCommand.get(context, KEY_ACCURACY, uid, category));
            Double la = parse(lat), lo = parse(lon);
            if (la != null && lo != null) {
                v.lat = la;
                v.lon = lo;
                v.set = !(la == 0d && lo == 0d);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Read location failed cat=" + category + " " + t);
        }
        return v;
    }

    private static String value(SettingPacket p) { return p == null ? null : p.value; }

    public static Double parse(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (Exception e) { return null; }
    }

    /** Random point within {@code meters} of the given point (uniform over the disc). */
    public static double[] jitter(double lat, double lon, double meters) {
        if (meters <= 0) return new double[]{lat, lon};
        double r = meters / 111000d;
        double w = r * Math.sqrt(Math.random());
        double t = 2 * Math.PI * Math.random();
        double dLon = w * Math.cos(t) / Math.max(0.01, Math.cos(Math.toRadians(lat)));
        double dLat = w * Math.sin(t);
        return new double[]{clampLat(lat + dLat), wrapLon(lon + dLon)};
    }

    public static double clampLat(double lat) { return Math.max(-85.05, Math.min(85.05, lat)); }

    public static double wrapLon(double lon) {
        while (lon > 180) lon -= 360;
        while (lon < -180) lon += 360;
        return lon;
    }

    /** Parses "lat, lon" (or "lat lon"); null when it is not a coordinate pair. */
    public static double[] parsePair(String q) {
        if (q == null) return null;
        String t = q.trim();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^\\s*([-+]?\\d{1,2}(?:\\.\\d+)?)\\s*[,;\\s]\\s*([-+]?\\d{1,3}(?:\\.\\d+)?)\\s*$")
                .matcher(t);
        if (!m.matches()) return null;
        try {
            double lat = Double.parseDouble(m.group(1));
            double lon = Double.parseDouble(m.group(2));
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return null;
            return new double[]{lat, lon};
        } catch (Exception e) {
            return null;
        }
    }
}
