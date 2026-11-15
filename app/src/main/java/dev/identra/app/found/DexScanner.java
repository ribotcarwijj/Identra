package dev.identra.app.found;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 0.6.0 static half of "Found in this app": reads the reference tables of every classes*.dex in an
 * APK (and its split APKs) and maps them to Identra hook ids. No code is parsed or run:
 * <ul>
 *   <li>method_ids / field_ids: every framework method or field the app's code refers to
 *       (e.g. Landroid/telephony/TelephonyManager;->getImei, Landroid/os/Build;->MODEL);</li>
 *   <li>string_ids: property names ("ro.product.model"), Settings keys ("android_id"), shell words used
 *       with getprop, AIDL descriptors and provider authorities that identify ID services.</li>
 * </ul>
 * Anything matched is "Likely": the app contains the code, it may or may not run it.
 */
public final class DexScanner {
    private static final String TAG = "Identra.FoundScan";
    /** Dex files larger than this are skipped (safety against absurd inputs). */
    private static final int MAX_DEX = 64 * 1024 * 1024;

    private DexScanner() { }

    public static final class Result {
        public final Set<String> ids = new LinkedHashSet<>();
        public int dexFiles, methodRefs, strings;
        public long millis;
        public String error;
    }

    /** Scans the base APK and splits. Blocking; call from a background thread. */
    public static Result scan(String baseApk, String[] splitApks) {
        long t0 = System.currentTimeMillis();
        Result r = new Result();
        FoundIndex index = FoundIndex.get();
        Matcher m = new Matcher(index);
        List<String> apks = new ArrayList<>();
        if (baseApk != null) apks.add(baseApk);
        if (splitApks != null) for (String s : splitApks) if (s != null) apks.add(s);
        for (String apk : apks) {
            if (!new File(apk).canRead()) continue;
            try (ZipFile zip = new ZipFile(apk)) {
                Enumeration<? extends ZipEntry> en = zip.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    String n = e.getName();
                    if (!n.startsWith("classes") || !n.endsWith(".dex") || n.contains("/")) continue;
                    if (e.getSize() > MAX_DEX) continue;
                    byte[] dex = readAll(zip.getInputStream(e), (int) Math.max(e.getSize(), 0));
                    try {
                        scanDex(dex, m, r);
                        r.dexFiles++;
                    } catch (Throwable t) {
                        Log.w(TAG, "dex " + n + " in " + apk + ": " + t);
                    }
                }
            } catch (Throwable t) {
                r.error = String.valueOf(t);
                Log.w(TAG, "apk " + apk + ": " + t);
            }
        }
        r.ids.addAll(m.out);
        // keys only count when the app has the API that reads them (keeps common words like "uuid" quiet)
        if (m.sawPropApi) r.ids.addAll(m.propHits);
        if (m.sawSettingsApi) r.ids.addAll(m.settingsHits);
        r.millis = System.currentTimeMillis() - t0;
        return r;
    }

    // ---- dex format (https://source.android.com/docs/core/runtime/dex-format) ----

    static void scanDex(byte[] d, Matcher m, Result r) {
        if (d.length < 0x70 || d[0] != 'd' || d[1] != 'e' || d[2] != 'x') throw new IllegalArgumentException("not a dex");
        int stringIdsSize = u4(d, 0x38), stringIdsOff = u4(d, 0x3C);
        int typeIdsSize = u4(d, 0x40), typeIdsOff = u4(d, 0x44);
        int fieldIdsSize = u4(d, 0x50), fieldIdsOff = u4(d, 0x54);
        int methodIdsSize = u4(d, 0x58), methodIdsOff = u4(d, 0x5C);

        // strings: decode every string once (keys / markers), keep descriptors lazily via cache below
        String[] cache = new String[stringIdsSize];
        for (int i = 0; i < stringIdsSize; i++) {
            String s = str(d, stringIdsOff, i, cache);
            m.string(s);
        }
        r.strings += stringIdsSize;

        // types that are hook target classes
        Map<Integer, Map<String, List<String>>> targetTypes = new HashMap<>();
        for (int t = 0; t < typeIdsSize; t++) {
            int descIdx = u4(d, typeIdsOff + t * 4);
            String desc = str(d, stringIdsOff, descIdx, cache);
            m.descriptor(desc);
            Map<String, List<String>> byName = m.classForDescriptor(desc);
            if (byName != null) targetTypes.put(t, byName);
        }
        if (targetTypes.isEmpty()) return;

        // method refs: class_idx u2, proto_idx u2, name_idx u4
        for (int i = 0; i < methodIdsSize; i++) {
            int off = methodIdsOff + i * 8;
            Map<String, List<String>> byName = targetTypes.get(u2(d, off));
            if (byName == null) continue;
            r.methodRefs++;
            String name = str(d, stringIdsOff, u4(d, off + 4), cache);
            add(byName.get(name), m.out);
            add(byName.get("*"), m.out);
        }
        // field refs: class_idx u2, type_idx u2, name_idx u4
        for (int i = 0; i < fieldIdsSize; i++) {
            int off = fieldIdsOff + i * 8;
            Map<String, List<String>> byName = targetTypes.get(u2(d, off));
            if (byName == null) continue;
            String name = str(d, stringIdsOff, u4(d, off + 4), cache);
            add(byName.get("#" + name), m.out);
        }
    }

    private static void add(List<String> ids, Set<String> out) { if (ids != null) out.addAll(ids); }

    private static String str(byte[] d, int stringIdsOff, int idx, String[] cache) {
        String s = cache[idx];
        if (s != null) return s;
        int off = u4(d, stringIdsOff + idx * 4);
        // uleb128 utf16 length, then MUTF-8 bytes terminated by 0
        int p = off;
        while ((d[p] & 0x80) != 0) p++;
        p++;
        int end = p;
        while (d[end] != 0) end++;
        s = mutf8(d, p, end);
        cache[idx] = s;
        return s;
    }

    private static String mutf8(byte[] d, int start, int end) {
        // fast path: ASCII
        boolean ascii = true;
        for (int i = start; i < end; i++) if ((d[i] & 0x80) != 0) { ascii = false; break; }
        if (ascii) return new String(d, start, end - start, java.nio.charset.StandardCharsets.ISO_8859_1);
        StringBuilder sb = new StringBuilder(end - start);
        int i = start;
        while (i < end) {
            int a = d[i++] & 0xFF;
            if (a < 0x80) sb.append((char) a);
            else if ((a & 0xE0) == 0xC0 && i < end) sb.append((char) (((a & 0x1F) << 6) | (d[i++] & 0x3F)));
            else if ((a & 0xF0) == 0xE0 && i + 1 < end) {
                sb.append((char) (((a & 0x0F) << 12) | ((d[i] & 0x3F) << 6) | (d[i + 1] & 0x3F)));
                i += 2;
            } else sb.append('?');
        }
        return sb.toString();
    }

    private static int u2(byte[] d, int o) { return (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8); }

    private static int u4(byte[] d, int o) {
        return (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8) | ((d[o + 2] & 0xFF) << 16) | ((d[o + 3] & 0xFF) << 24);
    }

    private static byte[] readAll(InputStream in, int hint) throws java.io.IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(hint > 0 ? hint : 1 << 20);
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toByteArray();
        }
    }

    /** Matching state for one scan (descriptor map + collected ids). */
    static final class Matcher {
        final FoundIndex index;
        final Map<String, Map<String, List<String>>> byDescriptor = new HashMap<>();
        final Set<String> out = new LinkedHashSet<>();
        final List<Map.Entry<String, List<String>>> markers;
        final List<Map.Entry<String, List<String>>> shell;
        final Set<String> propHits = new LinkedHashSet<>();
        final Set<String> settingsHits = new LinkedHashSet<>();
        boolean sawPropApi, sawSettingsApi;

        Matcher(FoundIndex index) {
            this.index = index;
            for (Map.Entry<String, Map<String, List<String>>> e : index.members.entrySet())
                byDescriptor.put("L" + e.getKey().replace('.', '/') + ";", e.getValue());
            markers = new ArrayList<>(index.markers.entrySet());
            shell = new ArrayList<>(index.shellWords.entrySet());
        }

        Map<String, List<String>> classForDescriptor(String desc) { return byDescriptor.get(desc); }

        void descriptor(String desc) {
            if (desc == null) return;
            switch (desc) {
                case "Landroid/os/SystemProperties;":
                case "Ljava/lang/Runtime;":
                case "Ljava/lang/ProcessBuilder;":
                    sawPropApi = true;
                    break;
                case "Landroid/provider/Settings$Secure;":
                case "Landroid/provider/Settings$Global;":
                case "Landroid/provider/Settings$System;":
                case "Landroid/provider/Settings$NameValueTable;":
                    sawSettingsApi = true;
                    break;
                default:
                    break;
            }
        }

        void string(String s) {
            if (s == null || s.length() < 3 || s.length() > 512) return;
            if (s.equals("android.os.SystemProperties")) sawPropApi = true;
            index.props.match(s, propHits);
            index.settingsKeys.match(s, settingsHits);
            // "getprop ro.x" style command strings
            if (s.startsWith("getprop")) {
                String[] parts = s.trim().split("\\s+");
                if (parts.length > 1) index.props.match(parts[1], out);
            }
            for (Map.Entry<String, List<String>> e : markers)
                if (s.contains(e.getKey())) out.addAll(e.getValue());
            // shell words only count as a command line ("cat /proc/meminfo", "stat -c", "ls -l")
            if (s.indexOf(' ') > 0 || s.startsWith("/proc/")) {
                String low = s.toLowerCase(java.util.Locale.ROOT);
                for (Map.Entry<String, List<String>> e : shell)
                    if (low.startsWith(e.getKey() + " ") || low.contains("/" + e.getKey()) || low.contains(" " + e.getKey() + " "))
                        out.addAll(e.getValue());
            }
        }
    }
}
