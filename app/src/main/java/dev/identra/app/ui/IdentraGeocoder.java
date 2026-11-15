package dev.identra.app.ui;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dev.identra.app.BuildConfig;

/**
 * Place search for the Location tab. Photon (komoot) first, Nominatim as fallback.
 * Blocking: call from a background thread. Only the typed query leaves the device.
 */
public final class IdentraGeocoder {
    private static final String TAG = "Identra.UiGeocoder";
    private static final String UA = BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME + " (Identra Android; location search)";

    private IdentraGeocoder() { }

    public static List<IdentraPlaces.Place> search(String query) throws Exception {
        Exception first = null;
        try {
            List<IdentraPlaces.Place> p = photon(query);
            if (!p.isEmpty()) return p;
        } catch (Exception e) {
            first = e;
            Log.w(TAG, "Photon failed: " + e);
        }
        try {
            return nominatim(query);
        } catch (Exception e) {
            Log.w(TAG, "Nominatim failed: " + e);
            throw first != null ? first : e;
        }
    }

    static List<IdentraPlaces.Place> photon(String q) throws Exception {
        String lang = Locale.getDefault().getLanguage();
        // Photon supports a few languages; others fall back to local names.
        boolean supported = "en".equals(lang) || "de".equals(lang) || "fr".equals(lang) || "it".equals(lang);
        String url = "https://photon.komoot.io/api/?q=" + Uri.encode(q) + "&limit=8" + (supported ? "&lang=" + lang : "");
        JSONObject root = new JSONObject(get(url));
        JSONArray features = root.optJSONArray("features");
        List<IdentraPlaces.Place> out = new ArrayList<>();
        if (features == null) return out;
        for (int i = 0; i < features.length(); i++) {
            JSONObject f = features.optJSONObject(i);
            if (f == null) continue;
            JSONObject geom = f.optJSONObject("geometry");
            JSONArray c = geom == null ? null : geom.optJSONArray("coordinates");
            JSONObject pr = f.optJSONObject("properties");
            if (c == null || c.length() < 2 || pr == null) continue;
            double lon = c.optDouble(0), lat = c.optDouble(1);
            if (Double.isNaN(lat) || Double.isNaN(lon)) continue;
            String name = pr.optString("name", "");
            String street = pr.optString("street", "");
            String house = pr.optString("housenumber", "");
            if (name.isEmpty()) name = (street + " " + house).trim();
            Set<String> parts = new LinkedHashSet<>();
            if (!street.isEmpty() && !name.contains(street)) parts.add((street + " " + house).trim());
            for (String k : new String[]{"city", "county", "state", "country"}) {
                String v = pr.optString(k, "");
                if (!v.isEmpty() && !v.equals(name)) parts.add(v);
            }
            String detail = join(parts);
            if (name.isEmpty()) name = detail.isEmpty() ? IdentraPlaces.shortCoords(lat, lon) : detail;
            out.add(new IdentraPlaces.Place(name, detail, lat, lon));
        }
        return out;
    }

    static List<IdentraPlaces.Place> nominatim(String q) throws Exception {
        String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=8&q=" + Uri.encode(q);
        JSONArray arr = new JSONArray(get(url));
        List<IdentraPlaces.Place> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            double lat, lon;
            try {
                lat = Double.parseDouble(o.optString("lat"));
                lon = Double.parseDouble(o.optString("lon"));
            } catch (Exception e) {
                continue;
            }
            String display = o.optString("display_name", "");
            String name = o.optString("name", "");
            if (name.isEmpty()) {
                int comma = display.indexOf(',');
                name = comma > 0 ? display.substring(0, comma) : display;
            }
            String detail = display.startsWith(name) ? display.substring(name.length()).replaceFirst("^,\\s*", "") : display;
            out.add(new IdentraPlaces.Place(name, detail, lat, lon));
        }
        return out;
    }

    private static String join(Set<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(p);
        }
        return sb.toString();
    }

    private static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(8000);
            c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent", UA);
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag());
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
            try (InputStream in = c.getInputStream();
                 BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } finally {
            c.disconnect();
        }
    }
}
