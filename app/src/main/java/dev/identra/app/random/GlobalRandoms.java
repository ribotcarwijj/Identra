package dev.identra.app.random;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import dev.identra.app.random.randomizers.NARandomizer;
import dev.identra.app.random.randomizers.Random3DigitNumber;
import dev.identra.app.random.randomizers.Random6DigitNumber;
import dev.identra.app.random.randomizers.RandomNetAddress6;
import dev.identra.app.random.randomizers.RandomNetGateway;
//import dev.identra.app.random.randomizers.RandomNetProvider;
import dev.identra.app.random.randomizers.RandomOAIDMiuiHyper;
import dev.identra.app.random.randomizers.RandomUUID;
import dev.identra.app.random.randomizers.RandomAlphaNumeric;
import dev.identra.app.random.randomizers.RandomAndroidID;
import dev.identra.app.random.randomizers.RandomAndroidVersion;
import dev.identra.app.random.randomizers.RandomBaseOs;
import dev.identra.app.random.randomizers.RandomBluetoothState;
import dev.identra.app.random.randomizers.RandomBoolean;
import dev.identra.app.random.randomizers.RandomBuildID;
import dev.identra.app.random.randomizers.RandomBuildTags;
import dev.identra.app.random.randomizers.RandomBuildType;
import dev.identra.app.random.randomizers.RandomBuildUser;
import dev.identra.app.random.randomizers.RandomCarrierName;
import dev.identra.app.random.randomizers.RandomDNS;
import dev.identra.app.random.randomizers.RandomDRM;
import dev.identra.app.random.randomizers.RandomDataState;
import dev.identra.app.random.randomizers.RandomDateOne;
import dev.identra.app.random.randomizers.RandomDateThree;
import dev.identra.app.random.randomizers.RandomDateTwo;
import dev.identra.app.random.randomizers.RandomDateZero;
import dev.identra.app.random.randomizers.RandomDevCodeName;
import dev.identra.app.random.randomizers.RandomFirebaseId;
import dev.identra.app.random.randomizers.RandomGSF;
import dev.identra.app.random.randomizers.RandomGameID;
import dev.identra.app.random.randomizers.RandomHostName;
import dev.identra.app.random.randomizers.RandomICCID;
import dev.identra.app.random.randomizers.RandomIMEI;
import dev.identra.app.random.randomizers.RandomKernelNodeName;
import dev.identra.app.random.randomizers.RandomKernelSysName;
import dev.identra.app.random.randomizers.RandomKernelRelease;
import dev.identra.app.random.randomizers.RandomKernelVersion;
import dev.identra.app.random.randomizers.RandomMAC;
import dev.identra.app.random.randomizers.RandomMEID;
import dev.identra.app.random.randomizers.RandomMSIN;
import dev.identra.app.random.randomizers.RandomManufacturer;
import dev.identra.app.random.randomizers.RandomMemory;
import dev.identra.app.random.randomizers.RandomNetAddress;
import dev.identra.app.random.randomizers.RandomNetD;
import dev.identra.app.random.randomizers.RandomNetworkType;
import dev.identra.app.random.randomizers.RandomPhoneNumber;
import dev.identra.app.random.randomizers.RandomPhoneType;
import dev.identra.app.random.randomizers.RandomSDKInit;
import dev.identra.app.random.randomizers.RandomSIMCount;
import dev.identra.app.random.randomizers.RandomSIMID;
import dev.identra.app.random.randomizers.RandomSIMState;
import dev.identra.app.random.randomizers.RandomSIMType;
import dev.identra.app.random.randomizers.RandomSSID;
import dev.identra.app.random.randomizers.RandomSerial;
import dev.identra.app.random.randomizers.RandomSimSerial;
import dev.identra.app.random.randomizers.RandomStringOne;
import dev.identra.app.random.randomizers.RandomSubUsage;
import dev.identra.app.random.randomizers.RandomSubscriberID;
import dev.identra.app.random.randomizers.RandomUserAgentManager;
import dev.identra.app.random.randomizers.RandomVoiceMailID;
import dev.identra.app.random.randomizers.RandomDateEpoch;
//import dev.identra.app.random.randomizers.RandomZoneParent;
import dev.identra.app.random.zone.RandomZoneDep;
import dev.identra.app.x.hook.interceptors.hardware.camera.RandomCameraCount;


public class GlobalRandoms {
    private static final Object lock = new Object();
    public static Map<String, IRandomizerOld> randomizers = new Hashtable<>();

    public static void putRandomizer(IRandomizerOld randomizer) { synchronized (lock) { randomizers.put(randomizer.getSettingName(), randomizer); } }
    public static void putRandomizers(List<IRandomizerOld> rs) {
        synchronized (lock) {
            for(IRandomizerOld r : rs)
                randomizers.put(r.getSettingName(), r);
        }
    }

    public static List<IRandomizerOld> getRandomizers() {
        synchronized (lock) {
            if(randomizers.isEmpty()) initRandomizers();
            List<IRandomizerOld> localCopy = new ArrayList<>(randomizers.values());
            localCopy.add(new RandomUserAgentManager());
            //localCopy.add(new RandomNetProvider());
            //localCopy.add(new RandomZoneParent());
            return localCopy;
        }
    }

    public static void initRandomizers() {
        putRandomizer(new NARandomizer());
        putRandomizer(new RandomOAIDMiuiHyper());
        putRandomizer(new RandomAndroidID());
        putRandomizer(new RandomDRM());
        putRandomizer(new RandomGSF());
        putRandomizer(new RandomIMEI());
        putRandomizer(new RandomMAC());
        putRandomizer(new RandomMEID());
        putRandomizer(new RandomUUID());
        putRandomizer(new RandomSimSerial());
        putRandomizer(new RandomSubscriberID());
        putRandomizer(new RandomSSID());
        putRandomizer(new RandomMemory());
        putRandomizer(new RandomDNS());
        putRandomizer(new RandomNetAddress());

        for(IRandomizerOld r : RandomNetAddress6.INSTANCES)
            putRandomizer(r);

        putRandomizer(new RandomNetGateway());
        putRandomizer(new RandomHostName());
        //putRandomizer(new RandomNetProvider());

        putRandomizer(new RandomSerial());
        putRandomizer(new RandomCarrierName());
        putRandomizer(new RandomMSIN());
        putRandomizer(new RandomSIMID());
        putRandomizer(new RandomSIMType());
        putRandomizer(new RandomSubUsage());
        putRandomizer(new RandomSIMState());
        putRandomizer(new RandomSIMCount());
        putRandomizer(new RandomAlphaNumeric());
        putRandomizer(new RandomPhoneType());
        putRandomizer(new Random3DigitNumber());
        putRandomizer(new RandomNetworkType());
        putRandomizer(new RandomDataState());
        putRandomizer(new RandomBluetoothState());
        putRandomizer(new RandomNetD());
        putRandomizer(new RandomICCID());
        putRandomizer(new RandomPhoneNumber());
        putRandomizer(new RandomVoiceMailID());
        putRandomizer(new RandomGameID());
        putRandomizer(new RandomBoolean());
        putRandomizer(new RandomManufacturer());
        putRandomizer(new RandomBaseOs());
        putRandomizer(new RandomDateZero());
        putRandomizer(new RandomDateEpoch());
        putRandomizer(new RandomDateOne());
        putRandomizer(new RandomDateTwo());
        putRandomizer(new RandomBuildID());
        putRandomizer(new RandomDevCodeName());
        putRandomizer(new RandomDateThree());
        putRandomizer(new RandomBuildTags());
        putRandomizer(new RandomBuildType());
        putRandomizer(new RandomBuildUser());
        putRandomizer(new RandomAndroidVersion());
        putRandomizer(new RandomSDKInit());
        putRandomizer(new RandomKernelRelease());
        putRandomizer(new RandomKernelNodeName());
        putRandomizer(new RandomKernelSysName());
        putRandomizer(new RandomKernelVersion());
        putRandomizer(new Random6DigitNumber());

        //
        //
        //

        //putRandomizer(new RandomGpsModelName());
        //putRandomizer(new RandomGpsModelYear());
        //putRandomizer(new RandomCameraCount());

        //
        //
        //



        //Collection Sort these ??????
        putRandomizers(RandomZoneDep.RANDOMIZERS);
        putRandomizer(new RandomFirebaseId());

        putRandomizer(new RandomStringOne());
    }

    public static boolean isRandomName(String name) { return "%random%".equalsIgnoreCase(name) || "%randomize%".equalsIgnoreCase(name); }
    public static void bindRandomToSettings(Map<String, String> settings) {
        List<IRandomizerOld> randomizers = getRandomizers();
        for(Map.Entry<String, String> s : settings.entrySet()) {
            if(isRandomName(s.getValue())) {
                for(IRandomizerOld r : randomizers) {
                    if(r.isSetting(s.getKey())) {
                        String nv = r.generateString();
                        settings.put(s.getKey(), nv);
                        break;
                    }
                }
            }
        }
    }
}
