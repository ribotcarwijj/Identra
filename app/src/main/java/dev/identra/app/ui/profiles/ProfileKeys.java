package dev.identra.app.ui.profiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.identra.app.R;

/**
 * 0.4.0: every setting a profile covers. Keys are the module's canonical setting names
 * (the ones in settingdefaults.json / the hooks' "settings" arrays, with slot 1 of the
 * indexed telephony keys). Legacy names the hooks still read (value.imei, gsm.operator.mcc,
 * zone.country.iso2, ...) are resolved to these by the hook side through remap_settings.json,
 * so writing the canonical key reaches both. Nothing here renames a key.
 */
public final class ProfileKeys {
    private ProfileKeys() { }

    public enum Section {
        DEVICE(R.string.identra_prof_sec_device, R.drawable.ic_identra_device),
        IDS(R.string.identra_prof_sec_ids, R.drawable.ic_identra_mark),
        PHONE(R.string.identra_prof_sec_phone, R.drawable.ic_identra_sim),
        NETWORK(R.string.identra_prof_sec_network, R.drawable.ic_identra_network),
        REGION(R.string.identra_prof_sec_region, R.drawable.ic_identra_globe),
        HARDWARE(R.string.identra_prof_sec_hardware, R.drawable.ic_identra_cpu),
        APPS(R.string.identra_prof_sec_apps, R.drawable.ic_identra_person);

        public final int title, icon;
        Section(int title, int icon) { this.title = title; this.icon = icon; }
    }

    public static final class Key {
        public final String name, label;
        public final Section section;
        Key(Section s, String name, String label) { this.section = s; this.name = name; this.label = label; }
    }

    private static final Map<String, Key> KEYS = new LinkedHashMap<>();

    private static void k(Section s, String name, String label) { KEYS.put(name, new Key(s, name, label)); }

    // ---- keys with a special meaning for the UI / generator ----
    public static final String BRAND = "device.brand", MANUFACTURER = "device.manufacturer", MODEL = "device.model";
    public static final String MARKET_NAME = "device.unique.name";
    public static final String COUNTRY_NAME = "zone.country.name", SIM_COUNTRY = "cell.sim.country.iso.1";
    public static final String LOC_TYPE = "location.type", LOC_LAT = "location.latitude", LOC_LON = "location.longitude", LOC_ACC = "location.accuracy";

    static {
        Section D = Section.DEVICE;
        k(D, "device.manufacturer", "Manufacturer");
        k(D, "device.brand", "Brand");
        k(D, "device.model", "Model");
        k(D, "device.unique.name", "Device name");
        k(D, "device.nick.name", "Device (Build.DEVICE)");
        k(D, "device.codename", "Boot device codename");
        k(D, "android.build.codename", "Product (Build.PRODUCT)");
        k(D, "device.bootloader", "Bootloader");
        k(D, "android.build.version", "Android version");
        k(D, "android.build.version.sdk", "SDK level");
        k(D, "android.build.patch", "Security patch");
        k(D, "android.build.id", "Build ID");
        k(D, "android.build.display.id", "Display build");
        k(D, "android.build.incremental", "Incremental");
        k(D, "android.build.fingerprint", "Fingerprint");
        k(D, "android.build.description", "Build description");
        k(D, "android.build.flavor", "Build flavor");
        k(D, "android.build.tags", "Build tags");
        k(D, "android.etc.build.rom.variant", "Build type");
        k(D, "android.etc.build.rom.user", "Build user");
        k(D, "android.etc.build.rom.version.codename", "Version codename");
        k(D, "android.build.host", "Build host");
        k(D, "android.build.date", "Build date");
        k(D, "android.build.date.epoc", "Build date (epoch s)");
        k(D, "android.build.date.utc", "Build time (Build.TIME, ms)");
        k(D, "android.kernel.sys.name", "Kernel name");
        k(D, "android.kernel.release", "Kernel release");
        k(D, "android.kernel.version", "Kernel version");
        k(D, "android.kernel.node.name", "Kernel node name");
        k(D, "settings.boot.count", "Boot count");
        k(D, "settings.adb.enabled", "ADB enabled");
        k(D, "settings.dev.settings.enabled", "Developer options");
        k(D, "settings.mock.location", "Mock location setting");
        k(D, "settings.usb.mass.storage", "USB mass storage");
        k(D, "settings.device.provisioned", "Device provisioned");
        k(D, "settings.stay.on.while.plugged", "Stay awake while charging");

        Section I = Section.IDS;
        k(I, "unique.android.id", "Android ID");
        k(I, "unique.gsf.id", "Google Services Framework ID");
        k(I, "unique.google.advertising.id", "Google advertising ID");
        k(I, "unique.google.app.set.id", "App set ID");
        k(I, "unique.facebook.advertising.id", "Facebook advertising ID");
        k(I, "unique.open.anon.advertising.id", "OAID");
        k(I, "unique.app.va.id", "VAID");
        k(I, "unique.app.anon.id", "AAID");
        k(I, "unique.guid.uuid", "GUID");
        k(I, "unique.serial.no", "Serial number");
        k(I, "unique.boot.id", "Boot ID");
        k(I, "unique.drm.id", "DRM (Widevine) ID");
        k(I, "analytics.firebase.instance.id", "Firebase instance ID");
        k(I, "props.unique.vbmeta.digest", "Verified boot digest");
        k(I, "props.unique.one.plus.serialno", "OnePlus serial");
        k(I, "settings.one.plus.security.uuid", "OnePlus security UUID");
        for (int i = 1; i <= 5; i++) k(I, "settings.unique.baidu.device.id." + i, "Baidu device ID " + i);
        k(I, "settings.xiaomi.extm.uuid", "Xiaomi extm UUID");
        k(I, "settings.xiaomi.mi.health.id", "Xiaomi health ID");
        k(I, "settings.xiaomi.gcbooster.uuid", "Xiaomi game booster UUID");
        k(I, "settings.xiaomi.key.mqs.uuid", "Xiaomi MQS UUID");
        k(I, "settings.xiaomi.mdm.uuid", "Xiaomi MDM UUID");
        k(I, "settings.unique.pay.session.id", "Pay session ID");

        Section P = Section.PHONE;
        k(P, "cell.phone.sim.card.count", "SIM slots");
        k(P, "cell.phone.type.1", "Phone type");
        k(P, "cell.unique.imei.1", "IMEI");
        k(P, "cell.unique.imei.2", "IMEI (slot 2)");
        k(P, "cell.unique.meid.1", "MEID");
        k(P, "cell.unique.phone.number.1", "Phone number");
        k(P, "cell.operator.name.1", "Operator");
        k(P, "cell.display.name.1", "SIM display name");
        k(P, "cell.operator.mcc.1", "MCC");
        k(P, "cell.operator.mnc.1", "MNC");
        k(P, "cell.operator.numeric.id.1", "Operator numeric (MCC+MNC)");
        k(P, "cell.operator.id.1", "Carrier ID");
        k(P, "cell.unique.msin.1", "MSIN");
        k(P, "cell.unique.subscriber.id.1", "IMSI");
        k(P, "cell.unique.sim.icc.id.1", "ICCID (SIM serial)");
        k(P, "cell.sim.country.iso.1", "SIM country");
        k(P, "cell.sim.subscription.id.1", "Subscription ID");
        k(P, "cell.sim.type.1", "SIM type");
        k(P, "cell.sim.is.embedded.1", "eSIM");
        k(P, "cell.sim.state.1", "SIM state");
        k(P, "cell.sim.state.flag.1", "SIM state code");
        k(P, "cell.voice.mail.alpha.tag.1", "Voicemail tag");
        k(P, "cell.display.name.source.1", "Display name source");
        k(P, "cell.operator.icon.tint.1", "SIM icon tint");
        k(P, "cell.data.roaming.enabled.flag.1", "Data roaming");
        k(P, "cell.data.is.opportunistic.1", "Opportunistic");
        k(P, "cell.location.lac", "Cell LAC");
        k(P, "cell.location.cid", "Cell ID");

        Section N = Section.NETWORK;
        k(N, "unique.network.mac.address", "Wi-Fi MAC");
        k(N, "unique.network.ssid", "Wi-Fi name (SSID)");
        k(N, "unique.network.bssid", "Router (BSSID)");
        k(N, "unique.bluetooth.address", "Bluetooth address");
        k(N, "device.unique.bluetooth.name", "Bluetooth name");
        k(N, "network.host.address", "Local IP address");
        k(N, "network.host.name", "Host name");
        k(N, "network.gateway", "Gateway");
        k(N, "network.dhcp.server", "DHCP server");
        k(N, "network.netmask", "Netmask");
        k(N, "network.dns", "DNS");
        k(N, "network.dns.list", "DNS list");
        k(N, "network.domains", "Search domains");

        Section R2 = Section.REGION;
        k(R2, "zone.country.name", "Country");
        k(R2, "zone.country.code", "Country code (ISO3)");
        k(R2, "zone.language.name", "Language");
        k(R2, "zone.language.iso", "Language code");
        k(R2, "zone.language.tag", "Locale");
        k(R2, "zone.timezone.id", "Time zone");
        k(R2, "zone.timezone.offset", "UTC offset");
        k(R2, "zone.timezone.display.name", "Time zone name");
        k(R2, "location.type", "Location mode");
        k(R2, "location.latitude", "Latitude");
        k(R2, "location.longitude", "Longitude");
        k(R2, "location.accuracy", "Accuracy (m)");

        Section H = Section.HARDWARE;
        k(H, "display.width", "Screen width (px)");
        k(H, "display.height", "Screen height (px)");
        k(H, "hardware.memory.total", "Memory total");
        k(H, "hardware.memory.available", "Memory available");
        k(H, "soc.board.model", "SoC model");
        k(H, "soc.board.manufacturer", "SoC manufacturer");
        k(H, "soc.board.manufacturer.id", "Hardware (Build.HARDWARE)");
        k(H, "soc.board.config.code.name", "Board / platform (Build.BOARD)");
        k(H, "soc.cpu.processor.count", "CPU cores");
        k(H, "soc.cpu.architecture", "CPU architecture");
        k(H, "soc.cpu.abi", "Primary ABI");
        k(H, "soc.cpu.abi.list", "ABI list");
        k(H, "soc.cpu.abi.list.32", "32-bit ABIs");
        k(H, "soc.cpu.abi.list.64", "64-bit ABIs");
        k(H, "soc.gpu.open.gles.vendor", "GPU vendor");
        k(H, "soc.gpu.open.gles.renderer", "GPU renderer");
        k(H, "soc.gpu.open.gles.version", "OpenGL ES version");
        k(H, "soc.gpu.open.gles.version.encoded", "OpenGL ES version code");
        k(H, "soc.gpu.egl.implementor", "EGL implementor");
        k(H, "soc.gpu.vulkan.implementor", "Vulkan implementor");
        k(H, "soc.baseband.board.radio.version", "Baseband (radio) version");
        k(H, "soc.baseband.board.config.name", "Baseband config");
        k(H, "soc.baseband.board.implementor", "RIL implementor");
        k(H, "hardware.camera.count", "Cameras");
        k(H, "battery.charge.percent", "Battery %");
        k(H, "battery.charging.cycles", "Battery cycles");
        k(H, "battery.is.charging.bool", "Charging");
        k(H, "battery.is.plugged.in.bool", "Plugged in");

        Section A = Section.APPS;
        k(A, "unique.account.email", "Account email");
        k(A, "user.agent", "Web user agent");
    }

    public static List<Key> all() { return new ArrayList<>(KEYS.values()); }

    public static List<String> names() { return new ArrayList<>(KEYS.keySet()); }

    public static Key get(String name) { return name == null ? null : KEYS.get(name); }

    public static boolean contains(String name) { return name != null && KEYS.containsKey(name); }

    public static List<Key> of(Section s) {
        List<Key> out = new ArrayList<>();
        for (Key k : KEYS.values()) if (k.section == s) out.add(k);
        return out;
    }

    public static String label(String name) {
        Key k = get(name);
        return k == null ? name : k.label;
    }

    /** Hook "settings" entries are base names ("cell.unique.imei") or legacy names; normalise for matching. */
    public static String baseOf(String name) {
        if (name == null) return "";
        String n = name.trim().toLowerCase(Locale.ROOT);
        return n.replaceAll("\\.\\[[0-9,]+]$", "").replaceAll("\\.[0-9]+$", "");
    }

    public static List<String> sortedNames() {
        List<String> l = names();
        Collections.sort(l);
        return l;
    }
}
