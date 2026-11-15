package dev.identra.app.ui.profiles;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * 0.4.0: real phone models so a generated identity is coherent (brand, model, Build.DEVICE,
 * Build.PRODUCT, board, SoC, GPU, screen, Android build all belong to the same phone). The
 * build ids and fingerprints follow each vendor's real format; values such as TAC prefixes
 * and incremental numbers are representative, not looked up per unit.
 */
public final class DeviceCatalog {
    private DeviceCatalog() { }

    public static final class Device {
        public String manufacturer, brand, model, marketName, device, product, board, hardware;
        public String socModel, socMaker, gpu, gpuVendor, egl;      // gpu = renderer string
        public String release, sdk, buildId, incremental, patch, kernel, bootloaderPrefix;
        public String baseband, ril, serialStyle, host, user;
        public long buildEpoch;
        public int width, height, ramGb, cores, cameras;
        public String[] tacs;

        public String fingerprint() {
            return brand + "/" + product + "/" + device + ":" + release + "/" + buildId + "/" + incremental + ":user/release-keys";
        }

        public String description() { return product + "-user " + release + " " + buildId + " " + incremental + " release-keys"; }

        public String summary() { return marketName; }
    }

    private static final List<Device> ALL = new ArrayList<>();

    private static Device d(String manufacturer, String brand, String model, String marketName, String device, String product,
                            String board, String hardware, String socModel, String socMaker, String gpu, String gpuVendor, String egl,
                            String release, String sdk, String buildId, String incremental, String patch, long buildEpoch,
                            String kernel, String bootloaderPrefix, String baseband, String ril,
                            int width, int height, int ramGb, int cores, int cameras, String serialStyle, String host, String user, String... tacs) {
        Device x = new Device();
        x.manufacturer = manufacturer; x.brand = brand; x.model = model; x.marketName = marketName; x.device = device; x.product = product;
        x.board = board; x.hardware = hardware; x.socModel = socModel; x.socMaker = socMaker; x.gpu = gpu; x.gpuVendor = gpuVendor; x.egl = egl;
        x.release = release; x.sdk = sdk; x.buildId = buildId; x.incremental = incremental; x.patch = patch; x.buildEpoch = buildEpoch;
        x.kernel = kernel; x.bootloaderPrefix = bootloaderPrefix; x.baseband = baseband; x.ril = ril;
        x.width = width; x.height = height; x.ramGb = ramGb; x.cores = cores; x.cameras = cameras;
        x.serialStyle = serialStyle; x.host = host; x.user = user; x.tacs = tacs;
        ALL.add(x);
        return x;
    }

    static {
        d("Google", "google", "Pixel 8 Pro", "Pixel 8 Pro", "husky", "husky", "zuma", "zuma", "Tensor G3", "Google",
                "Mali-G715", "ARM", "mali", "14", "34", "AP2A.240805.005", "12025142", "2024-08-05", 1720570000L,
                "5.15.137-android14-11-gb5a0a3a4b6b1-ab12025142", "ripcurrent-14.5-", "g5300q-240308-240517-B-11870224", "Shannon RIL",
                1344, 2992, 12, 9, 3, "pixel", "abfarm-release-2004-0115", "android-build", "35866963", "35190176");
        d("Google", "google", "Pixel 7", "Pixel 7", "panther", "panther", "gs201", "gs201", "Tensor G2", "Google",
                "Mali-G710", "ARM", "mali", "14", "34", "AP2A.240805.005", "12025142", "2024-08-05", 1720570000L,
                "5.10.198-android13-4-00050-g12f3388846c3-ab12025142", "cloudripper-14.5-", "g5300g-240308-240517-B-11870224", "Shannon RIL",
                1080, 2400, 8, 8, 2, "pixel", "abfarm-release-2004-0115", "android-build", "35214964", "35541983");
        d("Google", "google", "Pixel 6a", "Pixel 6a", "bluejay", "bluejay", "bluejay", "bluejay", "Tensor", "Google",
                "Mali-G78", "ARM", "mali", "14", "34", "AP2A.240805.005", "12025142", "2024-08-05", 1720570000L,
                "5.10.198-android13-4-00050-g12f3388846c3-ab12025142", "bluejay-14.5-", "g5123b-130914-240205-B-11405587", "Shannon RIL",
                1080, 2400, 6, 8, 2, "pixel", "abfarm-release-2004-0115", "android-build", "35165963", "35460612");
        d("samsung", "samsung", "SM-S911B", "Galaxy S23", "dm1q", "dm1qxxx", "kalama", "qcom", "SM8550", "QTI",
                "Adreno (TM) 740", "Qualcomm", "adreno", "14", "34", "UP1A.231005.007", "S911BXXS3CXD1", "2024-04-01", 1712620000L,
                "5.15.104-android13-8-28650522-abS911BXXS3CXD1", "S911BXXS3CXD1", "S911BXXS3CXD1", "Samsung RIL v1.4",
                1080, 2340, 8, 8, 3, "samsung", "SWDK4317", "dpi", "35099461", "35221265");
        d("samsung", "samsung", "SM-S928B", "Galaxy S24 Ultra", "e3q", "e3qxxx", "pineapple", "qcom", "SM8650", "QTI",
                "Adreno (TM) 750", "Qualcomm", "adreno", "14", "34", "UP1A.231005.007", "S928BXXS1AXD5", "2024-04-01", 1713150000L,
                "6.1.25-android14-11-28932130-abS928BXXS1AXD5", "S928BXXS1AXD5", "S928BXXS1AXD5", "Samsung RIL v1.4",
                1440, 3120, 12, 8, 5, "samsung", "SWDK7807", "dpi", "35070182", "35406578");
        d("samsung", "samsung", "SM-A546B", "Galaxy A54 5G", "a54x", "a54xnsxx", "s5e8835", "s5e8835", "S5E8835", "Samsung",
                "Mali-G68", "ARM", "mali", "14", "34", "UP1A.231005.007", "A546BXXS8CXD1", "2024-04-01", 1712700000L,
                "5.10.177-android12-9-28575149-abA546BXXS8CXD1", "A546BXXS8CXD1", "A546BXXS8CXD1", "Samsung RIL v1.4",
                1080, 2340, 8, 8, 4, "samsung", "SWDK4510", "dpi", "35337476", "35481693");
        d("Xiaomi", "Xiaomi", "2211133G", "Xiaomi 13", "fuxi", "fuxi_global", "kalama", "qcom", "SM8550", "QTI",
                "Adreno (TM) 740", "Qualcomm", "adreno", "14", "34", "UKQ1.230804.001", "V816.0.6.0.UMCMIXM", "2024-03-01", 1710300000L,
                "5.15.94-android13-8-o-g3b1e5c7b1c2f", "unknown", "MPSS.DE.3.1.1-00080-KAILUA_GEN_PACK-1", "Qualcomm RIL 1.0",
                1080, 2400, 8, 8, 3, "generic", "pangu-build-component-system-176203-0k7tq-s1lnr-cjrkr", "builder", "86882106", "86207506");
        d("Xiaomi", "Redmi", "23129RAA4G", "Redmi Note 13 5G", "gold", "gold_global", "mt6833", "mt6833", "MT6833V/ZA", "Mediatek",
                "Mali-G57 MC2", "ARM", "mali", "14", "34", "UP1A.230905.011", "V816.0.5.0.UNQMIXM", "2024-03-01", 1709800000L,
                "5.10.198-android12-9-o-g8c1d2f6a7b1e", "unknown", "MOLY.NR15.R3.MP1.TC8.PR3.SP.V1.P35", "mtk-ril 1.0",
                1080, 2400, 8, 8, 3, "generic", "pangu-build-component-system-175874-d6m2b-vnmbv-lg8v6", "builder", "86118907", "86765207");
        d("OnePlus", "OnePlus", "CPH2449", "OnePlus 11 5G", "OP594DL1", "CPH2449EEA", "kalama", "qcom", "SM8550", "QTI",
                "Adreno (TM) 740", "Qualcomm", "adreno", "14", "34", "UKQ1.230924.001", "U.R4T3.1b8a7c4_1-2b7f", "2024-05-01", 1714500000L,
                "5.15.123-android13-8-o-g2f6d5e8a1b44", "unknown", "Q_V1_P14,Q_V1_P14", "Qualcomm RIL 1.0",
                1440, 3216, 16, 8, 3, "oneplus", "dg02-pool02-kvm91", "root", "86280806", "86453307");
        d("motorola", "motorola", "moto g84 5G", "moto g84 5G", "bangkk", "bangkk_g", "holi", "qcom", "SM6375", "QTI",
                "Adreno (TM) 619", "Qualcomm", "adreno", "14", "34", "U1TCS34.17-87-4", "8d1b2c", "2024-04-01", 1713000000L,
                "5.4.254-qgki-g6a8b7c1d2e3f", "MBM-3.0-bangkk_g-", "M6375_14.140.03.61R BANGKK_EMEADSDS_CUST", "Qualcomm RIL 1.0",
                1080, 2400, 12, 8, 2, "motorola", "ilclbld67", "hudsoncm", "35873515", "35418680");
        d("Sony", "Sony", "XQ-DE54", "Xperia 5 V", "pdx237", "XQ-DE54_EEA", "kalama", "qcom", "SM8550", "QTI",
                "Adreno (TM) 740", "Qualcomm", "adreno", "14", "34", "68.1.A.2.170", "068001A002017000590963049", "2024-05-01", 1714700000L,
                "5.15.123-android13-8-gb4f0f5c2a9d1", "unknown", "1.0.1.0.1", "Qualcomm RIL 1.0",
                1080, 2520, 8, 8, 2, "sony", "BuildHost", "BuildUser", "35460115", "35880774");
        d("OPPO", "OPPO", "CPH2525", "OPPO Reno10 5G", "OP5A3BL1", "CPH2531EEA", "mt6877", "mt6877", "MT6877V/TTZA", "Mediatek",
                "Mali-G68 MC4", "ARM", "mali", "14", "34", "UP1A.231005.007", "U.1b5c0a4_1a2b3c", "2024-04-05", 1712900000L,
                "5.10.198-android12-9-o-g4d2c1b8e7f6a", "unknown", "MOLY.NR15.R3.TC10.PR2.SP.V1.P47", "mtk-ril 1.0",
                1080, 2412, 8, 8, 3, "generic", "dg02-pool03-kvm24", "root", "86754906", "86025407");
    }

    public static List<Device> all() { return new ArrayList<>(ALL); }

    public static Device random(SecureRandom r) { return ALL.get(r.nextInt(ALL.size())); }

    /** Best effort reverse lookup (for summaries of imported / hand-edited profiles). */
    public static Device byModel(String model) {
        if (model == null) return null;
        for (Device d : ALL) if (d.model.equalsIgnoreCase(model)) return d;
        return null;
    }
}
