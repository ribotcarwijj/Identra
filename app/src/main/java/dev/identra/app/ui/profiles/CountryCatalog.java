package dev.identra.app.ui.profiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 0.4.0: countries a profile can be generated for: mobile operators (MCC/MNC), phone number
 * shape, languages, time zones and a few cities, so SIM, locale, time zone and location agree.
 */
public final class CountryCatalog {
    private CountryCatalog() { }

    public static final class Operator {
        public final String name, mcc, mnc;
        Operator(String name, String mcc, String mnc) { this.name = name; this.mcc = mcc; this.mnc = mnc; }
    }

    public static final class City {
        public final String name;
        public final double lat, lon;
        City(String name, double lat, double lon) { this.name = name; this.lat = lat; this.lon = lon; }
    }

    public static final class Country {
        public String iso2, dial;
        public int nationalDigits;
        public String[] mobilePrefixes, languages, zones;
        public final List<Operator> operators = new ArrayList<>();
        public final List<City> cities = new ArrayList<>();

        public String name() { return new Locale("", iso2).getDisplayCountry(Locale.ENGLISH); }

        public String iso3() {
            try { return new Locale("", iso2).getISO3Country(); } catch (Exception e) { return iso2; }
        }

        public String flag() {
            if (iso2 == null || iso2.length() != 2) return "";
            int a = Character.codePointAt(iso2.toUpperCase(Locale.ROOT), 0) - 'A' + 0x1F1E6;
            int b = Character.codePointAt(iso2.toUpperCase(Locale.ROOT), 1) - 'A' + 0x1F1E6;
            return new String(Character.toChars(a)) + new String(Character.toChars(b));
        }
    }

    private static final List<Country> ALL = new ArrayList<>();

    private static Country c(String iso2, String dial, int nationalDigits, String prefixes, String langs, String zones) {
        Country c = new Country();
        c.iso2 = iso2; c.dial = dial; c.nationalDigits = nationalDigits;
        c.mobilePrefixes = prefixes.split(",");
        c.languages = langs.split(",");
        c.zones = zones.split(",");
        ALL.add(c);
        return c;
    }

    private static void op(Country c, String name, String mcc, String mnc) { c.operators.add(new Operator(name, mcc, mnc)); }

    private static void city(Country c, String name, double lat, double lon) { c.cities.add(new City(name, lat, lon)); }

    static {
        Country x;
        x = c("US", "1", 10, "202,212,213,305,312,415,503,617,646,702,713,818", "en-US", "America/New_York,America/Chicago,America/Denver,America/Los_Angeles");
        op(x, "T-Mobile", "310", "260"); op(x, "AT&T", "310", "410"); op(x, "Verizon", "311", "480");
        city(x, "New York", 40.7128, -74.0060); city(x, "Los Angeles", 34.0522, -118.2437); city(x, "Chicago", 41.8781, -87.6298);

        x = c("GB", "44", 10, "71,73,74,75,77,78,79", "en-GB", "Europe/London");
        op(x, "EE", "234", "30"); op(x, "Vodafone UK", "234", "15"); op(x, "O2 - UK", "234", "10"); op(x, "3", "234", "20");
        city(x, "London", 51.5074, -0.1278); city(x, "Manchester", 53.4808, -2.2426);

        x = c("DE", "49", 11, "151,152,157,160,162,163,170,171,172,173,175,176,177,178,179", "de-DE", "Europe/Berlin");
        op(x, "Telekom.de", "262", "01"); op(x, "Vodafone.de", "262", "02"); op(x, "o2 - de", "262", "03");
        city(x, "Berlin", 52.5200, 13.4050); city(x, "Munich", 48.1351, 11.5820); city(x, "Hamburg", 53.5511, 9.9937);

        x = c("FR", "33", 9, "6,7", "fr-FR", "Europe/Paris");
        op(x, "Orange F", "208", "01"); op(x, "SFR", "208", "10"); op(x, "Bouygues Telecom", "208", "20"); op(x, "Free", "208", "15");
        city(x, "Paris", 48.8566, 2.3522); city(x, "Lyon", 45.7640, 4.8357);

        x = c("ES", "34", 9, "6,7", "es-ES", "Europe/Madrid");
        op(x, "Movistar", "214", "07"); op(x, "Vodafone ES", "214", "01"); op(x, "Orange", "214", "03");
        city(x, "Madrid", 40.4168, -3.7038); city(x, "Barcelona", 41.3874, 2.1686);

        x = c("IT", "39", 10, "32,33,34,35,36,37,38,39", "it-IT", "Europe/Rome");
        op(x, "TIM", "222", "01"); op(x, "Vodafone IT", "222", "10"); op(x, "WINDTRE", "222", "88");
        city(x, "Rome", 41.9028, 12.4964); city(x, "Milan", 45.4642, 9.1900);

        x = c("NL", "31", 9, "6", "nl-NL", "Europe/Amsterdam");
        op(x, "KPN", "204", "08"); op(x, "Vodafone NL", "204", "04"); op(x, "Odido", "204", "16");
        city(x, "Amsterdam", 52.3676, 4.9041); city(x, "Rotterdam", 51.9244, 4.4777);

        x = c("SE", "46", 9, "70,72,73,76,79", "sv-SE", "Europe/Stockholm");
        op(x, "Telia", "240", "01"); op(x, "Tele2", "240", "07"); op(x, "Telenor SE", "240", "08"); op(x, "3 SE", "240", "02");
        city(x, "Stockholm", 59.3293, 18.0686); city(x, "Gothenburg", 57.7089, 11.9746); city(x, "Malmö", 55.6050, 13.0038);

        x = c("NO", "47", 8, "4,9", "nb-NO", "Europe/Oslo");
        op(x, "Telenor", "242", "01"); op(x, "Telia N", "242", "02");
        city(x, "Oslo", 59.9139, 10.7522); city(x, "Bergen", 60.3913, 5.3221);

        x = c("DK", "45", 8, "2,3,4,5,6,7", "da-DK", "Europe/Copenhagen");
        op(x, "TDC", "238", "01"); op(x, "Telenor DK", "238", "02"); op(x, "Telia DK", "238", "20"); op(x, "3 DK", "238", "06");
        city(x, "Copenhagen", 55.6761, 12.5683); city(x, "Aarhus", 56.1629, 10.2039);

        x = c("FI", "358", 9, "40,41,44,45,46,50", "fi-FI", "Europe/Helsinki");
        op(x, "Elisa", "244", "05"); op(x, "DNA", "244", "12"); op(x, "Telia FI", "244", "91");
        city(x, "Helsinki", 60.1699, 24.9384); city(x, "Tampere", 61.4978, 23.7610);

        x = c("PL", "48", 9, "50,51,53,57,60,66,69,72,78,79,88", "pl-PL", "Europe/Warsaw");
        op(x, "Plus", "260", "01"); op(x, "T-Mobile.pl", "260", "02"); op(x, "Orange", "260", "03"); op(x, "Play", "260", "06");
        city(x, "Warsaw", 52.2297, 21.0122); city(x, "Kraków", 50.0647, 19.9450);

        x = c("CH", "41", 9, "75,76,77,78,79", "de-CH,fr-CH", "Europe/Zurich");
        op(x, "Swisscom", "228", "01"); op(x, "Sunrise", "228", "02"); op(x, "Salt", "228", "03");
        city(x, "Zurich", 47.3769, 8.5417); city(x, "Geneva", 46.2044, 6.1432);

        x = c("AT", "43", 10, "650,660,664,676,680,681,688,699", "de-AT", "Europe/Vienna");
        op(x, "A1", "232", "01"); op(x, "Magenta", "232", "03"); op(x, "Drei", "232", "05");
        city(x, "Vienna", 48.2082, 16.3738); city(x, "Graz", 47.0707, 15.4395);

        x = c("IE", "353", 9, "83,85,86,87,89", "en-IE", "Europe/Dublin");
        op(x, "Vodafone IE", "272", "01"); op(x, "Three", "272", "05"); op(x, "eir", "272", "03");
        city(x, "Dublin", 53.3498, -6.2603); city(x, "Cork", 51.8985, -8.4756);

        x = c("CA", "1", 10, "416,438,514,587,604,613,647,778", "en-CA,fr-CA", "America/Toronto,America/Vancouver");
        op(x, "Rogers", "302", "720"); op(x, "Bell", "302", "610"); op(x, "TELUS", "302", "220");
        city(x, "Toronto", 43.6532, -79.3832); city(x, "Vancouver", 49.2827, -123.1207); city(x, "Montreal", 45.5019, -73.5674);

        x = c("AU", "61", 9, "4", "en-AU", "Australia/Sydney,Australia/Melbourne,Australia/Brisbane");
        op(x, "Telstra", "505", "01"); op(x, "Optus", "505", "02"); op(x, "Vodafone AU", "505", "03");
        city(x, "Sydney", -33.8688, 151.2093); city(x, "Melbourne", -37.8136, 144.9631);

        x = c("JP", "81", 10, "70,80,90", "ja-JP", "Asia/Tokyo");
        op(x, "NTT DOCOMO", "440", "10"); op(x, "SoftBank", "440", "20"); op(x, "au", "440", "50");
        city(x, "Tokyo", 35.6762, 139.6503); city(x, "Osaka", 34.6937, 135.5023);

        x = c("KR", "82", 10, "10", "ko-KR", "Asia/Seoul");
        op(x, "SKTelecom", "450", "05"); op(x, "KT", "450", "08"); op(x, "LG U+", "450", "06");
        city(x, "Seoul", 37.5665, 126.9780); city(x, "Busan", 35.1796, 129.0756);

        x = c("SG", "65", 8, "8,9", "en-SG", "Asia/Singapore");
        op(x, "Singtel", "525", "01"); op(x, "StarHub", "525", "05"); op(x, "M1", "525", "03");
        city(x, "Singapore", 1.3521, 103.8198);

        x = c("AE", "971", 9, "50,52,54,55,56,58", "en-AE,ar-AE", "Asia/Dubai");
        op(x, "Etisalat", "424", "02"); op(x, "du", "424", "03");
        city(x, "Dubai", 25.2048, 55.2708); city(x, "Abu Dhabi", 24.4539, 54.3773);

        x = c("IN", "91", 10, "6,7,8,9", "en-IN,hi-IN", "Asia/Kolkata");
        op(x, "Jio", "405", "857"); op(x, "airtel", "404", "10"); op(x, "Vi India", "404", "20");
        city(x, "Mumbai", 19.0760, 72.8777); city(x, "Delhi", 28.6139, 77.2090); city(x, "Bengaluru", 12.9716, 77.5946);

        x = c("BR", "55", 11, "119,219,319,419,519,619,719,819", "pt-BR", "America/Sao_Paulo");
        op(x, "VIVO", "724", "06"); op(x, "Claro BR", "724", "05"); op(x, "TIM", "724", "02");
        city(x, "São Paulo", -23.5505, -46.6333); city(x, "Rio de Janeiro", -22.9068, -43.1729);

        x = c("MX", "52", 10, "33,55,81", "es-MX", "America/Mexico_City");
        op(x, "Telcel", "334", "020"); op(x, "AT&T MX", "334", "050"); op(x, "Movistar", "334", "030");
        city(x, "Mexico City", 19.4326, -99.1332); city(x, "Guadalajara", 20.6597, -103.3496);

        x = c("TR", "90", 10, "50,53,54,55", "tr-TR", "Europe/Istanbul");
        op(x, "Turkcell", "286", "01"); op(x, "Vodafone TR", "286", "02"); op(x, "Türk Telekom", "286", "03");
        city(x, "Istanbul", 41.0082, 28.9784); city(x, "Ankara", 39.9334, 32.8597);
    }

    public static List<Country> all() {
        List<Country> l = new ArrayList<>(ALL);
        Collections.sort(l, (a, b) -> a.name().compareToIgnoreCase(b.name()));
        return l;
    }

    public static Country byIso(String iso2) {
        if (iso2 == null) return null;
        for (Country c : ALL) if (c.iso2.equalsIgnoreCase(iso2.trim())) return c;
        return null;
    }

    /** Lookup by English country name (zone.country.name) or ISO code. */
    public static Country find(String nameOrIso) {
        if (nameOrIso == null || nameOrIso.trim().isEmpty()) return null;
        Country c = byIso(nameOrIso);
        if (c != null) return c;
        for (Country x : ALL)
            if (x.name().equalsIgnoreCase(nameOrIso.trim()) || x.iso3().equalsIgnoreCase(nameOrIso.trim())) return x;
        return null;
    }

    public static Country random(java.util.Random r) { return ALL.get(r.nextInt(ALL.size())); }

    /** Flag emoji for any ISO2 code (also ones not in the catalog). */
    public static String flagOf(String iso2) {
        Country tmp = new Country();
        tmp.iso2 = iso2;
        return tmp.flag();
    }
}
