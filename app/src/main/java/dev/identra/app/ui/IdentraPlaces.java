package dev.identra.app.ui;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Local-only place lists for the Location tab: favourites, recently set places, and the
 * display name of what was last applied (globally and per app). Kept in this app's own
 * SharedPreferences as JSON; never sent to the module.
 */
public final class IdentraPlaces {
    private static final String FILE = "identra_places";
    private static final String KEY_FAV = "favourites";
    private static final String KEY_RECENT = "recent";
    private static final String KEY_NAMES = "applied_names";
    public static final String KEY_JITTER_ON = "jitter_on";
    public static final String KEY_JITTER_M = "jitter_m";
    public static final String KEY_MODE = "mode";
    public static final String KEY_ACCURACY = "accuracy";
    private static final int MAX_RECENT = 10;

    private IdentraPlaces() { }

    public static final class Place {
        public String name;
        public String detail;
        public double lat, lon;
        public long time;

        public Place() { }

        public Place(String name, String detail, double lat, double lon) {
            this.name = name;
            this.detail = detail;
            this.lat = lat;
            this.lon = lon;
        }

        public String coords() { return IdentraLocation.format(lat) + ", " + IdentraLocation.format(lon); }

        boolean same(Place o) { return o != null && Math.abs(o.lat - lat) < 1e-6 && Math.abs(o.lon - lon) < 1e-6; }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("name", name);
                if (detail != null) o.put("detail", detail);
                o.put("lat", lat);
                o.put("lon", lon);
                o.put("t", time);
            } catch (Exception ignored) { }
            return o;
        }

        static Place from(JSONObject o) {
            if (o == null || !o.has("lat") || !o.has("lon")) return null;
            Place p = new Place(o.optString("name", ""), o.optString("detail", null), o.optDouble("lat"), o.optDouble("lon"));
            p.time = o.optLong("t");
            if (Double.isNaN(p.lat) || Double.isNaN(p.lon)) return null;
            return p;
        }
    }

    public static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static List<Place> list(Context c, String key) {
        List<Place> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(key, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                Place p = Place.from(arr.optJSONObject(i));
                if (p != null) out.add(p);
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static void save(Context c, String key, List<Place> list) {
        JSONArray arr = new JSONArray();
        for (Place p : list) arr.put(p.toJson());
        prefs(c).edit().putString(key, arr.toString()).apply();
    }

    public static List<Place> favourites(Context c) { return list(c, KEY_FAV); }
    public static List<Place> recent(Context c) { return list(c, KEY_RECENT); }

    public static boolean isFavourite(Context c, Place p) {
        for (Place f : favourites(c)) if (f.same(p)) return true;
        return false;
    }

    public static void addFavourite(Context c, Place p) {
        List<Place> l = favourites(c);
        for (Place f : l) if (f.same(p)) return;
        l.add(0, p);
        save(c, KEY_FAV, l);
    }

    public static void removeFavourite(Context c, Place p) {
        List<Place> l = favourites(c);
        List<Place> keep = new ArrayList<>();
        for (Place f : l) if (!f.same(p)) keep.add(f);
        save(c, KEY_FAV, keep);
    }

    public static void renameFavourite(Context c, Place p, String name) {
        List<Place> l = favourites(c);
        for (Place f : l) if (f.same(p)) f.name = name;
        save(c, KEY_FAV, l);
    }

    public static void addRecent(Context c, Place p) {
        List<Place> l = recent(c);
        List<Place> out = new ArrayList<>();
        Place copy = new Place(p.name, p.detail, p.lat, p.lon);
        copy.time = System.currentTimeMillis();
        out.add(copy);
        for (Place r : l) {
            if (r.same(p)) continue;
            if (out.size() >= MAX_RECENT) break;
            out.add(r);
        }
        save(c, KEY_RECENT, out);
    }

    public static void clearRecent(Context c) { save(c, KEY_RECENT, new ArrayList<>()); }

    // ---- names of applied places (the module only stores coordinates) ----

    private static String scope(String pkg, int uid) { return pkg == null ? "global" : pkg + ":" + uid; }

    public static void rememberApplied(Context c, String pkg, int uid, Place p) {
        try {
            JSONObject o = new JSONObject(prefs(c).getString(KEY_NAMES, "{}"));
            o.put(scope(pkg, uid), p.toJson());
            prefs(c).edit().putString(KEY_NAMES, o.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static void forgetApplied(Context c, String pkg, int uid) {
        try {
            JSONObject o = new JSONObject(prefs(c).getString(KEY_NAMES, "{}"));
            o.remove(scope(pkg, uid));
            prefs(c).edit().putString(KEY_NAMES, o.toString()).apply();
        } catch (Exception ignored) { }
    }

    /** Name for coordinates read from the module, if they match what we last applied there. */
    public static String nameFor(Context c, String pkg, int uid, double lat, double lon) {
        try {
            JSONObject o = new JSONObject(prefs(c).getString(KEY_NAMES, "{}"));
            Place p = Place.from(o.optJSONObject(scope(pkg, uid)));
            if (p != null && Math.abs(p.lat - lat) < 0.05 && Math.abs(p.lon - lon) < 0.05 && p.name != null && !p.name.isEmpty())
                return p.name;
        } catch (Exception ignored) { }
        return null;
    }

    /** Last applied global place (cached locally; used by the Home dashboard without a service call). */
    public static Place appliedGlobal(Context c) {
        try {
            JSONObject o = new JSONObject(prefs(c).getString(KEY_NAMES, "{}"));
            return Place.from(o.optJSONObject("global"));
        } catch (Exception e) {
            return null;
        }
    }

    public static String shortCoords(double lat, double lon) {
        return String.format(Locale.US, "%.4f, %.4f", lat, lon);
    }
}
