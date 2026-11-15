package dev.identra.app.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import dev.identra.app.R;

/**
 * Maps internal hook group names to the friendly categories of the new UI.
 * Patterns ending in '*' are prefix matches; others are exact. Case-insensitive.
 * The most specific match wins (exact beats prefix, longer prefix beats shorter),
 * so e.g. "ID.Network*" (Network) wins over "ID.*" (Device) and
 * "Hide.VPN.State" (Network) wins over "Hide.*" (Stealth).
 */
public final class ProtectionCategories {
    private ProtectionCategories() { }

    public static final String OTHER = "other";

    public static final class Category {
        public final String key;
        public final int title;
        public final int desc;
        public final int icon;
        public final List<String> patterns;

        Category(String key, int title, int desc, int icon, String... patterns) {
            this.key = key;
            this.title = title;
            this.desc = desc;
            this.icon = icon;
            this.patterns = Collections.unmodifiableList(Arrays.asList(patterns));
        }
    }

    public static final List<Category> ALL;
    public static final Category OTHER_CATEGORY = new Category(OTHER,
            R.string.identra_cat_other, R.string.identra_cat_other_desc, R.drawable.ic_identra_more);

    static {
        List<Category> l = new ArrayList<>();
        l.add(new Category("device", R.string.identra_cat_device, R.string.identra_cat_device_desc, R.drawable.ic_identra_device,
                "ID.*", "Device.ID.*", "Hardware.*", "Spoof.Display", "Spoof.Battery", "Spoof.SOC", "Spoof.Features",
                "Storage.*", "Devices.Spoof.Info"));
        l.add(new Category("network", R.string.identra_cat_network, R.string.identra_cat_network_desc, R.drawable.ic_identra_network,
                "Network.*", "ID.Network*", "Hide.VPN.State", "Spoof.UserAgent", "Get.Bluetooth"));
        l.add(new Category("location", R.string.identra_cat_location, R.string.identra_cat_location_desc, R.drawable.ic_identra_location,
                "Spoof.Location*", "Spoof.TimeZone", "Spoof.Language", "Spoof.Status.Cell"));
        l.add(new Category("phone", R.string.identra_cat_phone, R.string.identra_cat_phone_desc, R.drawable.ic_identra_sim,
                "Spoof.Telephony", "ID.Telephony", "Get.Messages", "Send.Messages", "Get.Call.log"));
        l.add(new Category("apps", R.string.identra_cat_apps, R.string.identra_cat_apps_desc, R.drawable.ic_identra_apps,
                "Apps.*", "Spoof.Accounts", "Get.User", "User.Creation.Time",
                "Read.Sync", "Get.Contacts", "Spoof.Contacts", "Get.Calendars"));
        l.add(new Category("sensors", R.string.identra_cat_sensors, R.string.identra_cat_sensors_desc, R.drawable.ic_identra_sensors,
                "Use.Camera", "Record.*", "Get.Sensors", "Get.Clipboard", "Read.Notifications",
                "Get.Activity"));
        l.add(new Category("trackers", R.string.identra_cat_trackers, R.string.identra_cat_trackers_desc, R.drawable.ic_identra_trackers,
                "AD.*", "Ad.*", "Spoof.Embed.Paypal"));
        // Stealth is always shown last
        l.add(new Category("stealth", R.string.identra_cat_stealth, R.string.identra_cat_stealth_desc, R.drawable.ic_identra_stealth,
                "Hide.*", "Devices.Hide.*", "Intercept.*", "Settings.Spoof"));
        ALL = Collections.unmodifiableList(l);
    }

    /** Category key for an internal group name, or {@link #OTHER}. */
    public static String categoryOf(String group) {
        if (group == null) return OTHER;
        String g = group.trim().toLowerCase(Locale.ROOT);
        String best = OTHER;
        int bestScore = -1;
        for (Category c : ALL) {
            for (String p : c.patterns) {
                String lp = p.toLowerCase(Locale.ROOT);
                int score = -1;
                if (lp.endsWith("*")) {
                    String prefix = lp.substring(0, lp.length() - 1);
                    if (g.startsWith(prefix)) score = prefix.length();
                } else if (g.equals(lp)) {
                    score = 10000 + lp.length();
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = c.key;
                }
            }
        }
        return best;
    }

    public static Category byKey(String key) {
        for (Category c : ALL) if (c.key.equals(key)) return c;
        return OTHER_CATEGORY;
    }
}
