package dev.identra.app.nativeprops;

import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.identra.app.DebugUtil;
import dev.identra.app.x.hook.filter.PropRules;

/**
 * 0.7.0 native property layer (design: docs/90-native-props.md on branch ops).
 * <p>
 * Java lens and shell lens already read {@link PropRules}. This pushes the same rules into
 * libidentra_native.so, which hooks bionic's __system_property_get / _find / _read_callback /
 * _read / _foreach and popen("getprop ...") through the framework's native API, so NDK code sees
 * the same values as SystemProperties.get.
 * <p>
 * Runs only in hooked app processes (never system_server, never Identra itself). Every failure
 * leaves the process exactly as without the layer (pass-through).
 */
public final class NativeProps {
    private static final String TAG = "Identra.Native";
    public static final String LIB = "identra_native";
    /** Global/per-app setting to switch the native layer off ("false"). Default on. */
    public static final String SETTING_ENABLED = "identra.native.props";

    public static volatile String modulePath;
    private static volatile boolean loaded = false;
    private static volatile String loadError = null;

    private NativeProps() { }

    static native boolean nativeApiAvailable();
    static native int nativeApply(String[] keys, String[] values, String[] prefixes, String[] prefixValues, boolean debug);
    static native void nativeRemove();
    static native String nativeStatus();
    static native String nativeProbe(String key);

    private static synchronized boolean load() {
        if (loaded) return true;
        try {
            System.loadLibrary(LIB);
            loaded = true;
        } catch (Throwable t) {
            loadError = "loadLibrary: " + t;
            // module class loaders without a library path: load straight from the (uncompressed) APK
            String p = modulePath;
            if (p != null) {
                try {
                    System.load(p + "!/lib/" + Build.SUPPORTED_ABIS[0] + "/lib" + LIB + ".so");
                    loaded = true;
                    loadError = null;
                } catch (Throwable t2) {
                    loadError += " | load: " + t2;
                }
            }
        }
        if (!loaded) Log.w(TAG, "native library not loaded: " + loadError);
        return loaded;
    }

    /** Value the native side should return for a mapping, or null to leave the key alone. */
    private static String valueOf(Map<String, String> settings, String mapped) {
        if (mapped == null) return null;
        if (mapped.startsWith(PropRules.DIRECT)) return mapped.substring(PropRules.DIRECT.length());
        return settings.get(mapped);
    }

    /**
     * Builds the rule table from the app's resolved settings ("prop:" / "propprefix:" entries put
     * there by the property filter hooks) and hands it to the native layer.
     * @return number of rules pushed, 0 = nothing to do, -1 = native layer unavailable
     */
    public static int apply(String packageName, Map<String, String> settings) { return apply(packageName, settings, null); }

    /** Special keys in the exact table (never real property names): file overrides and uname fields. */
    public static final String FILE = "@file:";
    public static final String UNAME = "@uname:";
    public static final String BOOT_ID_PATH = "/proc/sys/kernel/random/boot_id";

    private static boolean hasHook(java.util.Set<String> assigned, String prefix) {
        if (assigned == null) return false;
        for (String id : assigned) if (id != null && id.startsWith(prefix)) return true;
        return false;
    }

    private static String nonEmpty(Map<String, String> settings, String name) {
        String v = settings.get(name);
        return v == null || v.trim().isEmpty() || v.contains("%") ? null : v.trim();
    }

    /**
     * 0.7.4: besides properties, the native side also answers file reads (open/openat/fopen and
     * popen "cat FILE") and uname() for the same values the Java hooks already return - only when
     * the matching Java hook is assigned, so the layers never disagree.
     */
    public static int apply(String packageName, Map<String, String> settings, java.util.Set<String> assigned) {
        if (packageName == null) return 0;
        Log.i(TAG, packageName + ": apply (settings " + (settings == null ? "null" : settings.size()) + ")");
        if (settings == null) return 0;
        if ("android".equals(packageName) || packageName.startsWith("dev.identra.app")) return 0;
        List<String> keys = new ArrayList<>(), vals = new ArrayList<>(), pfx = new ArrayList<>(), pfxVals = new ArrayList<>();
        synchronized (settings) {
            if ("false".equalsIgnoreCase(settings.get(SETTING_ENABLED))) { Log.i(TAG, packageName + ": native layer off (" + SETTING_ENABLED + ")"); return 0; }
            for (Map.Entry<String, String> e : settings.entrySet()) {
                String k = e.getKey();
                if (k == null) continue;
                if (k.startsWith(PropRules.PREFIX) && k.length() > PropRules.PREFIX.length()) {
                    String v = valueOf(settings, e.getValue());
                    if (v != null) { pfx.add(k.substring(PropRules.PREFIX.length())); pfxVals.add(v); }
                } else if (k.startsWith(PropRules.EXACT) && k.length() > PropRules.EXACT.length()) {
                    String v = valueOf(settings, e.getValue());
                    if (v != null) { keys.add(k.substring(PropRules.EXACT.length())); vals.add(v); }
                }
            }
        }
        int specials = 0;
        synchronized (settings) {
            // boot_id: same value the Java boot_id hooks hand out (createFakeUUIDFile uses unique.boot.id)
            String bootId = nonEmpty(settings, "unique.boot.id");
            if (bootId != null && hasHook(assigned, "Identra.boot_id/")) {
                keys.add(FILE + BOOT_ID_PATH); vals.add(bootId + "\n"); specials++;
            }
            // uname / /proc/version / osrelease: same values as the Java Os.uname hook
            String rel = nonEmpty(settings, "android.kernel.release");
            String ver = nonEmpty(settings, "android.kernel.version");
            if (rel != null && ver != null && hasHook(assigned, "Identra.Os.uname")) {
                keys.add(UNAME + "release"); vals.add(rel);
                keys.add(UNAME + "version"); vals.add(ver);
                String node = nonEmpty(settings, "android.kernel.node.name");
                if (node != null) { keys.add(UNAME + "nodename"); vals.add(node); }
                specials++;
            }
            // 0.7.7: GLES strings - same as the Java GLES*.glGetString hooks
            if (hasHook(assigned, "Identra.GPU/GLES20.glGetString")) {
                String[][] gl = {{"vendor", "soc.gpu.open.gles.vendor"}, {"renderer", "soc.gpu.open.gles.renderer"}, {"version", "soc.gpu.open.gles.version"}};
                for (String[] g : gl) {
                    String v = nonEmpty(settings, g[1]);
                    if (v != null) { keys.add("@gl:" + g[0]); vals.add(v); specials++; }
                }
            }
            // 0.7.7: /proc/meminfo - same generator the shell meminfo interceptor uses (MemTotal/MemAvailable)
            if (hasHook(assigned, "Identra.Filter/Shell/meminfo") || hasHook(assigned, "Identra.ActivityManager.getMemoryInfo")) {
                try {
                    int total = Integer.parseInt(nonEmpty(settings, "hardware.memory.total").trim());
                    int avail = Integer.parseInt(nonEmpty(settings, "hardware.memory.available").trim());
                    if (total < avail) total = avail;
                    String mem = dev.identra.app.utilities.MemoryUtil.generateFakeMeminfoContents(total, avail);
                    if (mem != null && !mem.isEmpty()) { keys.add(FILE + "/proc/meminfo"); vals.add(mem.endsWith("\n") ? mem : mem + "\n"); specials++; }
                } catch (Throwable ignored) { }
            }
        }
        Log.i(TAG, packageName + ": settings=" + settings.size() + " propRules exact=" + (keys.size() - specials) + " prefix=" + pfx.size()
                + " specials=" + specials + " sample=" + (keys.isEmpty() ? "-" : keys.get(0)));
        if (keys.isEmpty() && pfx.isEmpty()) return 0;
        if (!load()) return -1;
        try {
            if (!nativeApiAvailable()) {
                Log.w(TAG, packageName + ": framework did not call native_init (native API unavailable)");
                return -1;
            }
            int n = nativeApply(keys.toArray(new String[0]), vals.toArray(new String[0]),
                    pfx.toArray(new String[0]), pfxVals.toArray(new String[0]), DebugUtil.isDebug());
            Log.i(TAG, packageName + ": native rules=" + n + " (" + keys.size() + " exact, " + pfx.size() + " prefix)");
            if (n > 0) selfCheck(packageName);
            return n;
        } catch (Throwable t) {
            Log.w(TAG, packageName + ": native apply failed: " + t);
            return -1;
        }
    }

    private static void selfCheck(String packageName) {
        try {
            for (String k : new String[]{"ro.product.model", "ro.build.fingerprint", "ro.soc.model"})
                Log.i(TAG, packageName + " probe " + k + " " + nativeProbe(k));
            Log.i(TAG, packageName + " status " + nativeStatus());
        } catch (Throwable ignored) { }
    }

    /** Old generation before a hot reload: hand libc back untouched. */
    public static void remove() {
        if (!loaded) return;
        try { nativeRemove(); } catch (Throwable ignored) { }
    }

    public static String status() {
        if (!loaded) return "not loaded" + (loadError == null ? "" : " (" + loadError + ")");
        try { return nativeStatus(); } catch (Throwable t) { return "error " + t; }
    }
}
