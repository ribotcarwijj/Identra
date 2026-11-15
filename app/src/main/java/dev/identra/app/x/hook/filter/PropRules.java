package dev.identra.app.x.hook.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 0.5.0: one place that decides what a system property read returns, shared by the Java lens
 * (SystemProperties.get / System.getProperty hooks) and the shell lens (getprop through
 * Runtime.exec / ProcessBuilder), so both always agree.
 *
 * Rules come from "Intercept.Properties" rule hooks (see GetPropFilterContainer):
 *  - "prop:&lt;key&gt;"        -> setting name whose value replaces the property
 *  - "propprefix:&lt;prefix&gt;" -> same for every key starting with prefix (rule param "prefix*")
 *  - a setting name "val:&lt;literal&gt;" means the literal itself; "val:" (empty) hides the property
 *
 * Never fabricates: a property that is empty / missing on the real device is returned as is,
 * so a spoof can not add keys the real phone does not have (that is a detection flag).
 * Prefix rules win over exact rules so a "hide the real vendor's properties" rule cannot be
 * undone by a vendor id spoof rule for one of those keys.
 */
public final class PropRules {
    public static final String EXACT = "prop:";
    public static final String PREFIX = "propprefix:";
    public static final String DIRECT = FilterContainerElement.DIRECT_VAL_SYMBOL;

    private PropRules() { }

    private static final Map<Map<String, String>, List<String[]>> PREFIX_CACHE = new WeakHashMap<>();

    private static List<String[]> prefixes(Map<String, String> settings) {
        synchronized (PREFIX_CACHE) {
            List<String[]> l = PREFIX_CACHE.get(settings);
            if (l != null) return l;
            l = new ArrayList<>();
            synchronized (settings) {
                for (Map.Entry<String, String> e : settings.entrySet()) {
                    String k = e.getKey();
                    if (k != null && k.startsWith(PREFIX) && k.length() > PREFIX.length())
                        l.add(new String[]{k.substring(PREFIX.length()), e.getValue()});
                }
            }
            // longest prefix first
            Collections.sort(l, (a, b) -> b[0].length() - a[0].length());
            PREFIX_CACHE.put(settings, l);
            return l;
        }
    }

    /** The setting name (or "val:..." literal) a property is mapped to, or null. */
    public static String mappingOf(Map<String, String> settings, String property) {
        if (settings == null || property == null || property.isEmpty()) return null;
        for (String[] p : prefixes(settings))
            if (property.startsWith(p[0])) return p[1];
        synchronized (settings) {
            return settings.get(EXACT + property);
        }
    }

    public static boolean hasRule(Map<String, String> settings, String property) {
        return mappingOf(settings, property) != null;
    }

    /**
     * The value to return for a property instead of the real one, or null to leave the real
     * value untouched. "" means hidden (reads as a missing property).
     */
    public static String resolve(Map<String, String> settings, String property, String realValue) {
        if (realValue == null || realValue.isEmpty()) return null;   // never fabricate a key
        String mapped = mappingOf(settings, property);
        if (mapped == null) return null;
        if (mapped.startsWith(DIRECT)) return mapped.substring(DIRECT.length());
        String v;
        synchronized (settings) {
            v = settings.get(mapped);
        }
        if (v == null) return null;            // profile/global value not set: keep the real one
        return v.equals(realValue) ? null : v;
    }

    /**
     * Rewrites "getprop" listing output ("[key]: [value]" lines, also after grep). Returns the
     * new text; lines of hidden properties are dropped. Other lines are kept byte for byte.
     */
    public static String rewriteListing(Map<String, String> settings, String output) {
        if (output == null || output.isEmpty() || settings == null) return output;
        StringBuilder sb = new StringBuilder(output.length());
        int start = 0, n = output.length();
        while (start < n) {
            int nl = output.indexOf('\n', start);
            int end = nl < 0 ? n : nl + 1;
            String line = output.substring(start, end);
            start = end;
            String body = line;
            String eol = "";
            if (body.endsWith("\n")) { eol = "\n"; body = body.substring(0, body.length() - 1); }
            if (body.endsWith("\r")) { eol = "\r" + eol; body = body.substring(0, body.length() - 1); }
            int kEnd = body.indexOf("]: [");
            if (body.startsWith("[") && kEnd > 1 && body.endsWith("]")) {
                String key = body.substring(1, kEnd);
                String value = body.substring(kEnd + 4, body.length() - 1);
                String nv = resolve(settings, key, value);
                if (nv != null) {
                    if (nv.isEmpty()) continue;    // hidden
                    sb.append('[').append(key).append("]: [").append(nv).append(']').append(eol);
                    continue;
                }
            }
            sb.append(line);
        }
        return sb.toString();
    }
}
