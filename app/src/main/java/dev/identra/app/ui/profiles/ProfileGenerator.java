package dev.identra.app.ui.profiles;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import dev.identra.app.x.core.settings.NameInformation;
import dev.identra.app.x.core.settings.SettingHolder;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.ui.core.view_registry.SharedRegistry;

/**
 * 0.4.0: builds coherent profile values. A device comes from {@link DeviceCatalog} and a country
 * from {@link CountryCatalog}; every identifier is derived from them (IMEI with the device's TAC
 * and a Luhn digit, IMSI/ICCID/phone number from the operator and country, locale and time zone
 * from the country, Bluetooth name from the model, user agent from model + build). Keys without
 * a dedicated generator fall back to the module's own randomizers (RandomizersCache, the same
 * ones the classic editor's dice button uses), run headless through RandomizerSessionContext.
 *
 * Locked keys and keys outside the requested scope are never touched, and they pin the device /
 * country / operator so a partial re-randomize stays consistent with what is kept.
 */
public final class ProfileGenerator {
    private static final String TAG = "Identra.ProfileGen";
    private static final SecureRandom R = new SecureRandom();

    private ProfileGenerator() { }

    /** Context the per-key generators share. */
    private static final class Ctx {
        Context android;
        DeviceCatalog.Device device;
        CountryCatalog.Country country;
        CountryCatalog.Operator operator;
        Profile profile;
        Set<String> scope;
        final java.util.Map<String, String> out = new java.util.HashMap<>();
        String lang, zone;

        /** value already decided in this run, or the profile's current one */
        String cur(String key) {
            String v = out.get(key);
            return v != null ? v : profile.get(key);
        }
    }

    /**
     * Fills {@code keys} (null = every key) of the profile, skipping locked keys when
     * {@code respectLocks}. {@code countryIso} forces a country (null = keep/choose).
     */
    public static void randomize(Context context, Profile p, Collection<String> keys, String countryIso, boolean respectLocks) {
        Ctx c = new Ctx();
        c.android = context;
        c.profile = p;
        c.scope = new HashSet<>(keys == null ? ProfileKeys.names() : keys);
        if (respectLocks) c.scope.removeAll(p.locked);

        // ---- device: pinned when the model is kept, else random ----
        DeviceCatalog.Device pinned = DeviceCatalog.byModel(p.get(ProfileKeys.MODEL));
        boolean deviceFree = c.scope.contains(ProfileKeys.MODEL) && c.scope.contains(ProfileKeys.BRAND);
        c.device = (!deviceFree && pinned != null) ? pinned : (deviceFree ? DeviceCatalog.random(R) : (pinned != null ? pinned : DeviceCatalog.random(R)));

        // ---- country ----
        CountryCatalog.Country forced = countryIso == null ? null : CountryCatalog.byIso(countryIso);
        CountryCatalog.Country kept = CountryCatalog.find(p.countryIso());
        boolean countryFree = c.scope.contains(ProfileKeys.COUNTRY_NAME) && c.scope.contains(ProfileKeys.SIM_COUNTRY);
        if (forced != null) c.country = forced;
        else if (kept != null && !countryFree) c.country = kept;
        else c.country = countryFree || kept == null ? CountryCatalog.random(R) : kept;

        // ---- operator: keep when MCC/MNC are kept and belong to the country ----
        String mcc = p.get("cell.operator.mcc.1"), mnc = p.get("cell.operator.mnc.1");
        boolean opFree = c.scope.contains("cell.operator.mcc.1") || c.scope.contains("cell.operator.mnc.1");
        CountryCatalog.Operator keptOp = null;
        for (CountryCatalog.Operator o : c.country.operators) if (o.mcc.equals(mcc) && o.mnc.equals(mnc)) keptOp = o;
        c.operator = (!opFree && keptOp != null) ? keptOp : c.country.operators.get(R.nextInt(c.country.operators.size()));

        // ---- language / zone ----
        String keptTag = p.get("zone.language.tag");
        c.lang = (!c.scope.contains("zone.language.tag") && !keptTag.isEmpty() && kept == c.country) ? keptTag
                : c.country.languages[R.nextInt(c.country.languages.length)];
        String keptZone = p.get("zone.timezone.id");
        c.zone = (!c.scope.contains("zone.timezone.id") && !keptZone.isEmpty() && kept == c.country) ? keptZone
                : c.country.zones[R.nextInt(c.country.zones.length)];

        // order matters: identifiers derived from others are generated after them
        List<String> order = ProfileKeys.names();
        for (String key : order) {
            if (!c.scope.contains(key)) continue;
            String v = null;
            try { v = generate(c, key); } catch (Throwable t) { Log.w(TAG, "gen " + key + " failed: " + t); }
            if (v == null) v = existingRandomizer(context, key);
            if (v == null) v = fallback(key);
            if (v != null) c.out.put(key, v);
        }
        for (java.util.Map.Entry<String, String> e : c.out.entrySet()) if (c.scope.contains(e.getKey())) p.put(e.getKey(), e.getValue());
        if (c.scope.contains(ProfileKeys.COUNTRY_NAME) || forced != null || p.country == null || p.country.isEmpty())
            p.country = c.country.iso2;
    }

    // ---------------------------------------------------------------------------------

    private static String generate(Ctx c, String key) {
        DeviceCatalog.Device d = c.device;
        CountryCatalog.Country co = c.country;
        CountryCatalog.Operator op = c.operator;
        switch (key) {
            // device
            case "device.manufacturer": return d.manufacturer;
            case "device.brand": return d.brand;
            case "device.model": return d.model;
            case "device.unique.name": return d.marketName;
            case "device.nick.name": return d.device;
            case "device.codename": return d.device;
            case "android.build.codename": return d.product;
            case "device.bootloader":
                return d.bootloaderPrefix.endsWith("-") ? d.bootloaderPrefix + digits(8) : d.bootloaderPrefix;
            case "android.build.version": return d.release;
            case "android.build.version.sdk": return d.sdk;
            case "android.build.patch": return d.patch;
            case "android.build.id": return d.buildId;
            case "android.build.display.id":
                return "samsung".equalsIgnoreCase(d.brand) ? d.buildId + "." + d.incremental : d.buildId;
            case "android.build.incremental": return d.incremental;
            case "android.build.fingerprint": return d.fingerprint();
            case "android.build.description": return d.description();
            case "android.build.flavor": return d.product + "-user";
            case "android.build.tags": return "release-keys";
            case "android.etc.build.rom.variant": return "user";
            case "android.etc.build.rom.user": return d.user;
            case "android.etc.build.rom.version.codename": return "REL";
            case "android.build.host": return d.host;
            case "android.build.date": return buildDate(d.buildEpoch);
            case "android.build.date.epoc": return String.valueOf(d.buildEpoch);
            case "android.build.date.utc": return String.valueOf(d.buildEpoch * 1000L);
            case "android.kernel.sys.name": return "Linux";
            case "android.kernel.release": return d.kernel;
            case "android.kernel.version": return "#1 SMP PREEMPT " + buildDate(d.buildEpoch - 3600);
            case "android.kernel.node.name": return "localhost";
            case "settings.boot.count": return String.valueOf(3 + R.nextInt(80));
            case "settings.adb.enabled":
            case "settings.dev.settings.enabled":
            case "settings.mock.location":
            case "settings.usb.mass.storage":
            case "settings.stay.on.while.plugged": return "0";
            case "settings.device.provisioned": return "1";

            // identifiers
            case "unique.android.id": return nonZeroHex(16);
            case "unique.gsf.id": return nonZeroHex(16);
            case "unique.google.advertising.id":
            case "unique.google.app.set.id":
            case "unique.facebook.advertising.id":
            case "unique.open.anon.advertising.id":
            case "unique.app.va.id":
            case "unique.app.anon.id":
            case "unique.guid.uuid":
            case "unique.boot.id":
            case "settings.one.plus.security.uuid":
            case "settings.xiaomi.extm.uuid":
            case "settings.xiaomi.gcbooster.uuid":
            case "settings.xiaomi.key.mqs.uuid":
            case "settings.xiaomi.mdm.uuid":
            case "settings.unique.pay.session.id": return UUID.randomUUID().toString();
            case "unique.serial.no": return serial(d);
            case "unique.drm.id": return hex(64);
            case "props.unique.vbmeta.digest": return hex(64);
            case "props.unique.one.plus.serialno": return hex(8);
            case "settings.xiaomi.mi.health.id": return digits(16);
            case "analytics.firebase.instance.id": return firebaseId();

            // phone & SIM
            case "cell.phone.sim.card.count": return "1";
            case "cell.phone.type.1": return "1";
            case "cell.unique.imei.1":
            case "cell.unique.imei.2": return imei(d);
            case "cell.unique.meid.1": {
                String imei = c.cur("cell.unique.imei.1");
                return imei.length() >= 14 ? imei.substring(0, 14) : imei(d).substring(0, 14);
            }
            case "cell.unique.phone.number.1": return phone(co);
            case "cell.operator.name.1":
            case "cell.display.name.1": return op.name;
            case "cell.operator.mcc.1": return op.mcc;
            case "cell.operator.mnc.1": return op.mnc;
            case "cell.operator.numeric.id.1": return op.mcc + op.mnc;
            case "cell.unique.msin.1": return digits(15 - op.mcc.length() - op.mnc.length());
            case "cell.unique.subscriber.id.1": {
                String msin = c.cur("cell.unique.msin.1");
                if (msin.length() != 15 - op.mcc.length() - op.mnc.length()) msin = digits(15 - op.mcc.length() - op.mnc.length());
                return op.mcc + op.mnc + msin;
            }
            case "cell.unique.sim.icc.id.1": return iccid(co);
            case "cell.sim.country.iso.1": return co.iso2.toLowerCase(Locale.ROOT);
            case "cell.sim.subscription.id.1": return "1";
            case "cell.sim.type.1": return "0";
            case "cell.sim.is.embedded.1": return "false";
            case "cell.sim.state.1": return "READY";
            case "cell.sim.state.flag.1": return "5";
            case "cell.voice.mail.alpha.tag.1": return "Voicemail";
            case "cell.data.roaming.enabled.flag.1": return "0";
            case "cell.data.is.opportunistic.1": return "false";
            case "cell.location.lac": return String.valueOf(1000 + R.nextInt(60000));
            case "cell.location.cid": return String.valueOf(10000 + R.nextInt(268000000));

            // network
            case "unique.network.mac.address": return mac(true);
            case "unique.network.bssid": return mac(false);
            case "unique.bluetooth.address": return mac(false);
            case "unique.network.ssid": return ssid(op);
            case "device.unique.bluetooth.name": {
                String n = c.cur("device.unique.name");
                return n.isEmpty() ? d.marketName : n;
            }
            case "network.host.address": {
                String gw = c.cur("network.gateway");
                String base = gw.matches("\\d+\\.\\d+\\.\\d+\\.1") ? gw.substring(0, gw.lastIndexOf('.')) : subnet();
                return base + "." + (2 + R.nextInt(240));
            }
            case "network.gateway":
            case "network.dhcp.server":
            case "network.dns":
            case "network.dns.list": {
                String host = c.out.get("network.host.address");
                if (host == null) host = c.profile.get("network.host.address");
                if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) return host.substring(0, host.lastIndexOf('.')) + ".1";
                return subnet() + ".1";
            }
            case "network.netmask": return "255.255.255.0";
            case "network.domains": return "lan";
            case "network.host.name": {
                String n = c.cur("device.unique.name");
                if (n.isEmpty()) n = d.marketName;
                return n.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-|-$", "");
            }

            // region
            case "zone.country.name": return co.name();
            case "zone.country.code": return co.iso3();
            case "zone.language.tag": return c.lang;
            case "zone.language.iso": return Locale.forLanguageTag(c.lang).getLanguage();
            case "zone.language.name": return Locale.forLanguageTag(c.lang).getDisplayLanguage(Locale.ENGLISH);
            case "zone.timezone.id": return c.zone;
            case "zone.timezone.offset": return gmtOffset(c.zone);
            case "zone.timezone.display.name": return TimeZone.getTimeZone(c.zone).getDisplayName(false, TimeZone.LONG, Locale.ENGLISH);
            case "location.type": return "set";
            case "location.latitude":
            case "location.longitude": {
                double[] ll = pointFor(c);
                return String.format(Locale.US, "%.6f", key.endsWith("latitude") ? ll[0] : ll[1]);
            }
            case "location.accuracy": return null == c.profile.values.get(key) ? "" : c.profile.get(key);

            // display & hardware
            case "display.width": return String.valueOf(d.width);
            case "display.height": return String.valueOf(d.height);
            case "hardware.memory.total": return String.valueOf(d.ramGb);
            case "hardware.memory.available": return String.valueOf(Math.max(1, Math.round(d.ramGb * (0.25 + R.nextDouble() * 0.3))));
            case "soc.board.model": return d.socModel;
            case "soc.board.manufacturer": return d.socMaker;
            case "soc.board.manufacturer.id": return d.hardware;
            case "soc.board.config.code.name": return d.board;
            case "soc.cpu.processor.count": return String.valueOf(d.cores);
            case "soc.cpu.architecture": return "aarch64";
            case "soc.cpu.abi": return "arm64-v8a";
            case "soc.cpu.abi.list": return only64(d) ? "arm64-v8a" : "arm64-v8a,armeabi-v7a,armeabi";
            case "soc.cpu.abi.list.32": return only64(d) ? "" : "armeabi-v7a,armeabi";
            case "soc.cpu.abi.list.64": return "arm64-v8a";
            case "soc.gpu.open.gles.vendor": return d.gpuVendor;
            case "soc.gpu.open.gles.renderer": return d.gpu;
            case "soc.gpu.open.gles.version": return glesVersion(d);
            case "soc.gpu.open.gles.version.encoded": return "196610";
            case "soc.gpu.egl.implementor":
            case "soc.gpu.vulkan.implementor": return d.egl;
            case "soc.baseband.board.radio.version": return d.baseband;
            case "soc.baseband.board.config.name": return d.board;
            case "soc.baseband.board.implementor": return d.ril;
            case "hardware.camera.count": return String.valueOf(d.cameras);
            case "battery.charge.percent": return String.valueOf(30 + R.nextInt(66));
            case "battery.charging.cycles": return String.valueOf(40 + R.nextInt(560));
            case "battery.is.charging.bool":
            case "battery.is.plugged.in.bool": return "false";

            // apps & accounts
            case "unique.account.email": return email();
            case "user.agent": {
                String model = c.cur("device.model"), id = c.cur("android.build.id"), rel = c.cur("android.build.version");
                if (model.isEmpty()) model = d.model;
                if (id.isEmpty()) id = d.buildId;
                if (rel.isEmpty()) rel = d.release;
                return "Mozilla/5.0 (Linux; Android " + rel + "; " + model + " Build/" + id
                        + "; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/126.0.6478.122 Mobile Safari/537.36";
            }
            default:
                if (key.startsWith("settings.unique.baidu.device.id.")) return hex(32).toUpperCase(Locale.ROOT);
                return null;
        }
    }

    /** Last resort when neither a dedicated generator nor the module's randomizer produced a value. */
    private static String fallback(String key) {
        switch (key) {
            case "cell.operator.id.1": return String.valueOf(1 + R.nextInt(2600));
            case "cell.display.name.source.1": return "0";
            case "cell.operator.icon.tint.1": {
                int[] tints = {0xFF0B8043, 0xFF1A73E8, 0xFFE37400, 0xFF8430CE, 0xFFD93025, 0xFF00796B};
                return String.valueOf(tints[R.nextInt(tints.length)]);
            }
            default: return null;
        }
    }

    // ---- the module's own randomizers, headless -------------------------------------

    /** Runs the classic editor's randomizer for one key; null when there is none or it fails. */
    public static String existingRandomizer(Context context, String key) {
        try {
            Map<String, IRandomizer> map = RandomizersCache.getCopy();
            if (!map.containsKey(key)) return null;
            SettingHolder h = new SettingHolder(NameInformation.create(key), "", null);
            List<SettingHolder> list = new ArrayList<>(Collections.singletonList(h));
            RandomizerSessionContext.create().randomize(list, list, context, new SharedRegistry());
            String v = h.getNewValue();
            return v == null || v.trim().isEmpty() ? null : v.trim();
        } catch (Throwable t) {
            Log.w(TAG, "randomizer " + key + " failed: " + t);
            return null;
        }
    }

    // ---- helpers ------------------------------------------------------------------------

    private static boolean only64(DeviceCatalog.Device d) {
        return "husky".equals(d.device) || "panther".equals(d.device);
    }

    private static String buildDate(long epochSeconds) {
        SimpleDateFormat f = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(epochSeconds * 1000L));
    }

    private static String gmtOffset(String zone) {
        int mins = TimeZone.getTimeZone(zone).getRawOffset() / 60000;
        String sign = mins < 0 ? "-" : "+";
        mins = Math.abs(mins);
        return "GMT" + sign + (mins / 60) + (mins % 60 == 0 ? "" : String.format(Locale.US, ":%02d", mins % 60));
    }

    private static double[] pointFor(Ctx c) {
        String lat = c.out.get("location.latitude");
        String lon = c.out.get("location.longitude");
        if (lat != null && lon != null) return new double[]{Double.parseDouble(lat), Double.parseDouble(lon)};
        CountryCatalog.City city = c.country.cities.get(R.nextInt(c.country.cities.size()));
        double[] p = jitter(city.lat, city.lon, 3000);
        c.out.put("location.latitude", String.format(Locale.US, "%.6f", p[0]));
        c.out.put("location.longitude", String.format(Locale.US, "%.6f", p[1]));
        return p;
    }

    private static double[] jitter(double lat, double lon, double meters) {
        double r = meters / 111000d;
        double w = r * Math.sqrt(R.nextDouble());
        double t = 2 * Math.PI * R.nextDouble();
        return new double[]{lat + w * Math.sin(t), lon + w * Math.cos(t) / Math.max(0.01, Math.cos(Math.toRadians(lat)))};
    }

    public static String digits(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(R.nextInt(10));
        return sb.toString();
    }

    private static String hex(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append("0123456789abcdef".charAt(R.nextInt(16)));
        return sb.toString();
    }

    private static String nonZeroHex(int n) {
        String h = hex(n);
        return h.charAt(0) == '0' ? "1" + h.substring(1) : h;
    }

    private static String alnumUpper(int n) {
        String a = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(a.charAt(R.nextInt(a.length())));
        return sb.toString();
    }

    /** Luhn check digit for a numeric string. */
    public static int luhn(String body) {
        int sum = 0;
        boolean dbl = true;
        for (int i = body.length() - 1; i >= 0; i--) {
            int n = body.charAt(i) - '0';
            if (dbl) { n *= 2; if (n > 9) n -= 9; }
            sum += n;
            dbl = !dbl;
        }
        return (10 - sum % 10) % 10;
    }

    public static boolean luhnValid(String s) {
        if (s == null || !s.matches("\\d{2,}")) return false;
        return luhn(s.substring(0, s.length() - 1)) == s.charAt(s.length() - 1) - '0';
    }

    private static String imei(DeviceCatalog.Device d) {
        String tac = d.tacs[R.nextInt(d.tacs.length)];
        String body = tac + digits(6);
        return body + luhn(body);
    }

    private static String iccid(CountryCatalog.Country co) {
        String cc = co.dial.length() == 1 ? co.dial + "0" : co.dial;   // E.164 code, 2-3 digits
        String body = "89" + cc + digits(2) + digits(18 - 2 - cc.length() - 2);
        return body + luhn(body);
    }

    private static String phone(CountryCatalog.Country co) {
        String prefix = co.mobilePrefixes[R.nextInt(co.mobilePrefixes.length)];
        int rest = Math.max(0, co.nationalDigits - prefix.length());
        String national = prefix + digits(rest);
        if ("1".equals(co.dial) && national.length() == 10 && (national.charAt(3) == '0' || national.charAt(3) == '1'))
            national = national.substring(0, 3) + (2 + R.nextInt(8)) + national.substring(4);
        return "+" + co.dial + national;
    }

    private static String mac(boolean randomized) {
        byte[] b = new byte[6];
        R.nextBytes(b);
        b[0] = (byte) (randomized ? ((b[0] & 0xFC) | 0x02) : (b[0] & 0xFC));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) sb.append(':');
            sb.append(String.format(Locale.US, "%02x", b[i] & 0xFF));
        }
        return sb.toString();
    }

    private static String subnet() {
        String[] s = {"192.168.0", "192.168.1", "192.168.1", "192.168.178", "192.168.68", "192.168.100", "10.0.0"};
        return s[R.nextInt(s.length)];
    }

    private static String ssid(CountryCatalog.Operator op) {
        String h = alnumUpper(4);
        switch (R.nextInt(6)) {
            case 0: return op.name.replaceAll("[^A-Za-z0-9]", "") + "-" + h;
            case 1: return "TP-Link_" + h;
            case 2: return "NETGEAR" + (10 + R.nextInt(89));
            case 3: return "ASUS_" + h.substring(0, 2) + "_5G";
            case 4: return "Home-" + h;
            default: return "Wi-Fi-" + h;
        }
    }

    private static String serial(DeviceCatalog.Device d) {
        switch (d.serialStyle) {
            case "pixel": return (10 + R.nextInt(89)) + digits(3) + alnumUpper(9);
            case "samsung": return "R5C" + alnumUpper(8);
            case "motorola": return "ZY22" + alnumUpper(6);
            case "sony": return "QV7" + alnumUpper(7);
            default: return hex(8);
        }
    }

    private static String glesVersion(DeviceCatalog.Device d) {
        if ("adreno".equals(d.egl))
            return "OpenGL ES 3.2 V@0" + (600 + R.nextInt(200)) + "." + R.nextInt(70) + " (GIT@" + hex(10) + ", I" + hex(10) + ", " + (d.buildEpoch - 86400 * 30) + ") (Date:"
                    + new SimpleDateFormat("MM/dd/yy", Locale.US).format(new Date((d.buildEpoch - 86400 * 30) * 1000L)) + ")";
        return "OpenGL ES 3.2 v1.r" + (38 + R.nextInt(8)) + "p" + R.nextInt(2) + "-01eac0." + hex(32);
    }

    private static String firebaseId() {
        String a = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        StringBuilder sb = new StringBuilder();
        sb.append("cdef".charAt(R.nextInt(4)));
        for (int i = 1; i < 22; i++) sb.append(a.charAt(R.nextInt(a.length())));
        return sb.toString();
    }

    private static String email() {
        String[] first = {"alex", "sam", "jamie", "chris", "robin", "taylor", "jordan", "casey", "morgan", "kim", "lee", "nova", "eli", "max", "noah", "mia", "leo", "ella"};
        String[] last = {"berg", "miller", "lind", "novak", "rossi", "martin", "smith", "garcia", "nilsson", "weber", "dubois", "kowalski", "silva", "lopez"};
        String sep = R.nextBoolean() ? "." : "";
        return first[R.nextInt(first.length)] + sep + last[R.nextInt(last.length)] + (10 + R.nextInt(989)) + "@gmail.com";
    }

    // ---- from this phone ----------------------------------------------------------------

    /** Real values of this phone where Android lets a normal app read them (no IMEI etc.). */
    @SuppressWarnings("deprecation")
    public static void fromThisDevice(Context context, Profile p) {
        p.put("device.manufacturer", Build.MANUFACTURER);
        p.put("device.brand", Build.BRAND);
        p.put("device.model", Build.MODEL);
        p.put("device.nick.name", Build.DEVICE);
        p.put("device.codename", Build.DEVICE);
        p.put("android.build.codename", Build.PRODUCT);
        p.put("device.bootloader", Build.BOOTLOADER);
        p.put("android.build.version", Build.VERSION.RELEASE);
        p.put("android.build.version.sdk", String.valueOf(Build.VERSION.SDK_INT));
        p.put("android.build.patch", Build.VERSION.SECURITY_PATCH);
        p.put("android.build.id", Build.ID);
        p.put("android.build.display.id", Build.DISPLAY);
        p.put("android.build.incremental", Build.VERSION.INCREMENTAL);
        p.put("android.build.fingerprint", Build.FINGERPRINT);
        p.put("android.build.tags", Build.TAGS);
        p.put("android.etc.build.rom.variant", Build.TYPE);
        p.put("android.etc.build.rom.user", Build.USER);
        p.put("android.etc.build.rom.version.codename", Build.VERSION.CODENAME);
        p.put("android.build.host", Build.HOST);
        p.put("android.build.date.utc", String.valueOf(Build.TIME));
        p.put("android.build.date.epoc", String.valueOf(Build.TIME / 1000L));
        p.put("android.build.date", buildDate(Build.TIME / 1000L));
        p.put("android.kernel.sys.name", "Linux");
        p.put("android.kernel.release", System.getProperty("os.version"));
        p.put("soc.board.config.code.name", Build.BOARD);
        p.put("soc.board.manufacturer.id", Build.HARDWARE);
        if (Build.VERSION.SDK_INT >= 31) {
            p.put("soc.board.model", Build.SOC_MODEL);
            p.put("soc.board.manufacturer", Build.SOC_MANUFACTURER);
        }
        p.put("soc.cpu.processor.count", String.valueOf(Runtime.getRuntime().availableProcessors()));
        p.put("soc.cpu.architecture", System.getProperty("os.arch"));
        if (Build.SUPPORTED_ABIS.length > 0) p.put("soc.cpu.abi", Build.SUPPORTED_ABIS[0]);
        p.put("soc.cpu.abi.list", android.text.TextUtils.join(",", Build.SUPPORTED_ABIS));
        p.put("soc.cpu.abi.list.32", android.text.TextUtils.join(",", Build.SUPPORTED_32_BIT_ABIS));
        p.put("soc.cpu.abi.list.64", android.text.TextUtils.join(",", Build.SUPPORTED_64_BIT_ABIS));
        try { p.put("soc.baseband.board.radio.version", Build.getRadioVersion()); } catch (Throwable ignored) { }
        try {
            String name = Settings.Global.getString(context.getContentResolver(), "device_name");
            p.put("device.unique.name", name);
            p.put("device.unique.bluetooth.name", name);
        } catch (Throwable ignored) { }
        try { p.put("unique.android.id", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)); } catch (Throwable ignored) { }
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/sys/kernel/random/boot_id"))) {
            p.put("unique.boot.id", r.readLine());
        } catch (Throwable ignored) { }
        try {
            DisplayMetrics m = new DisplayMetrics();
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getRealMetrics(m);
            p.put("display.width", String.valueOf(Math.min(m.widthPixels, m.heightPixels)));
            p.put("display.height", String.valueOf(Math.max(m.widthPixels, m.heightPixels)));
        } catch (Throwable ignored) { }
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            p.put("hardware.memory.total", String.valueOf(Math.max(1, Math.round(mi.totalMem / 1073741824.0))));
            p.put("hardware.memory.available", String.valueOf(Math.max(1, Math.round(mi.availMem / 1073741824.0))));
        } catch (Throwable ignored) { }
        Locale loc = Locale.getDefault();
        p.put("zone.language.tag", loc.toLanguageTag());
        p.put("zone.language.iso", loc.getLanguage());
        p.put("zone.language.name", loc.getDisplayLanguage(Locale.ENGLISH));
        TimeZone tz = TimeZone.getDefault();
        p.put("zone.timezone.id", tz.getID());
        p.put("zone.timezone.offset", gmtOffset(tz.getID()));
        p.put("zone.timezone.display.name", tz.getDisplayName(false, TimeZone.LONG, Locale.ENGLISH));
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String simOp = tm.getSimOperator();
            if (simOp != null && simOp.length() >= 5) {
                p.put("cell.operator.mcc.1", simOp.substring(0, 3));
                p.put("cell.operator.mnc.1", simOp.substring(3));
                p.put("cell.operator.numeric.id.1", simOp);
            }
            p.put("cell.operator.name.1", tm.getSimOperatorName());
            p.put("cell.display.name.1", tm.getSimOperatorName());
            String iso = tm.getSimCountryIso();
            if (iso != null && !iso.isEmpty()) {
                p.put("cell.sim.country.iso.1", iso.toLowerCase(Locale.ROOT));
                p.country = iso.toUpperCase(Locale.ROOT);
            }
        } catch (Throwable ignored) { }
        String iso = p.country != null && !p.country.isEmpty() ? p.country : loc.getCountry();
        if (iso != null && !iso.isEmpty()) {
            Locale cl = new Locale("", iso);
            p.put("zone.country.name", cl.getDisplayCountry(Locale.ENGLISH));
            try { p.put("zone.country.code", cl.getISO3Country()); } catch (Throwable ignored) { }
            if (p.country == null || p.country.isEmpty()) p.country = iso.toUpperCase(Locale.ROOT);
        }
    }
}
