package dev.identra.app.ui.profiles;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 0.4.0: one complete device identity. Values are keyed by the module's setting names (see {@link ProfileKeys}). */
public final class Profile {
    public String id;
    public String name;
    /** ISO2 of the country it was generated for (may be empty). */
    public String country = "";
    public long created, updated;
    /** 0.7.22: an unsaved identity made for one app copy; not listed under Profiles, deleted when no app uses it. */
    public boolean temp;
    public final Map<String, String> values = new LinkedHashMap<>();
    /** Keys kept when re-randomizing. */
    public final Set<String> locked = new LinkedHashSet<>();

    public static Profile create(String name) {
        Profile p = new Profile();
        p.id = newId();
        p.name = name;
        p.created = p.updated = System.currentTimeMillis();
        return p;
    }

    public static String newId() { return UUID.randomUUID().toString().replace("-", "").substring(0, 16); }

    public String get(String key) {
        String v = values.get(key);
        return v == null ? "" : v;
    }

    public boolean has(String key) {
        String v = values.get(key);
        return v != null && !v.trim().isEmpty();
    }

    public void put(String key, String value) {
        if (value == null || value.trim().isEmpty()) values.remove(key);
        else values.put(key, value.trim());
    }

    public boolean isLocked(String key) { return locked.contains(key); }

    public void setLocked(String key, boolean on) { if (on) locked.add(key); else locked.remove(key); }

    public int filledCount() {
        int n = 0;
        for (String k : ProfileKeys.names()) if (has(k)) n++;
        return n;
    }

    /** "Galaxy S23 · samsung" style line. */
    public String deviceSummary() {
        String market = get(ProfileKeys.MARKET_NAME);
        String model = get(ProfileKeys.MODEL);
        String brand = get(ProfileKeys.BRAND);
        if (market.isEmpty() && model.isEmpty() && brand.isEmpty()) return "";
        String head = !market.isEmpty() ? market : model;
        if (!brand.isEmpty() && !head.toLowerCase().contains(brand.toLowerCase())) head = cap(brand) + " " + head;
        return head.trim();
    }

    public String countryIso() {
        if (country != null && !country.isEmpty()) return country;
        String sim = get(ProfileKeys.SIM_COUNTRY);
        if (!sim.isEmpty()) return sim.toUpperCase();
        CountryCatalog.Country c = CountryCatalog.find(get(ProfileKeys.COUNTRY_NAME));
        return c == null ? "" : c.iso2;
    }

    public String countryLabel() {
        String iso = countryIso();
        String name = get(ProfileKeys.COUNTRY_NAME);
        if (name.isEmpty() && !iso.isEmpty()) name = new java.util.Locale("", iso).getDisplayCountry(java.util.Locale.ENGLISH);
        if (name.isEmpty()) return "";
        return (iso.isEmpty() ? "" : CountryCatalog.flagOf(iso) + " ") + name;
    }

    private static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    public Profile copy(String newName) {
        Profile p = create(newName);
        p.country = country;
        p.values.putAll(values);
        p.locked.addAll(locked);
        return p;
    }

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("country", country == null ? "" : country);
        o.put("created", created);
        o.put("updated", updated);
        if (temp) o.put("temp", true);
        JSONObject v = new JSONObject();
        for (Map.Entry<String, String> e : values.entrySet()) v.put(e.getKey(), e.getValue());
        o.put("values", v);
        JSONArray l = new JSONArray();
        for (String k : locked) l.put(k);
        o.put("locked", l);
        return o;
    }

    public static Profile fromJson(JSONObject o) {
        Profile p = new Profile();
        p.id = o.optString("id", newId());
        if (p.id.isEmpty() || !p.id.matches("[A-Za-z0-9_-]{1,40}")) p.id = newId();
        p.name = o.optString("name", "Profile");
        p.country = o.optString("country", "");
        p.created = o.optLong("created", System.currentTimeMillis());
        p.updated = o.optLong("updated", p.created);
        p.temp = o.optBoolean("temp", false);
        JSONObject v = o.optJSONObject("values");
        if (v != null) {
            Iterator<String> it = v.keys();
            while (it.hasNext()) {
                String k = it.next();
                String val = v.optString(k, "");
                if (!val.isEmpty()) p.values.put(k, val);
            }
        }
        JSONArray l = o.optJSONArray("locked");
        if (l != null) for (int i = 0; i < l.length(); i++) p.locked.add(l.optString(i));
        return p;
    }
}
