package dev.identra.app.ui.tools;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** 0.7.24: stable instance names ("Instance 2" stays 2 when 1 is removed; the user can rename). */
public final class InstanceNames {
    private static final String PREFS = "identra_instances";

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Name of the instance living in clone user {@code user}; assigns the lowest free number the first time. */
    public static synchronized String get(Context c, int user) {
        SharedPreferences p = prefs(c);
        String n = p.getString("name_" + user, null);
        if (n != null) return n;
        Set<String> used = new HashSet<>();
        for (String k : p.getAll().keySet()) if (k.startsWith("name_")) used.add(p.getString(k, ""));
        int i = 1;
        while (used.contains("Instance " + i)) i++;
        n = "Instance " + i;
        p.edit().putString("name_" + user, n).apply();
        return n;
    }

    public static synchronized void set(Context c, int user, String name) {
        prefs(c).edit().putString("name_" + user, name).apply();
    }

    public static synchronized void forget(Context c, int user) {
        prefs(c).edit().remove("name_" + user).apply();
    }

    /** Drop names of users that are gone. */
    public static synchronized void keepOnly(Context c, Collection<Integer> users) {
        SharedPreferences p = prefs(c);
        SharedPreferences.Editor e = p.edit();
        for (String k : p.getAll().keySet()) {
            if (!k.startsWith("name_")) continue;
            try { if (!users.contains(Integer.parseInt(k.substring(5)))) e.remove(k); } catch (Throwable ignored) { }
        }
        e.apply();
    }
}
