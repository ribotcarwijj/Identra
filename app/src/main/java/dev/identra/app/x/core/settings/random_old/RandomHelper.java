package dev.identra.app.x.core.settings.random_old;

import java.util.HashMap;
import java.util.Map;

import dev.identra.app.x.hook.interceptors.build.random.RandomBaseOs;
import dev.identra.app.x.hook.interceptors.build.random.RandomBuildCodeName;
import dev.identra.app.x.hook.interceptors.build.random.RandomBuildId;
import dev.identra.app.x.hook.interceptors.build.random.RandomBuildRadio;
import dev.identra.app.x.hook.interceptors.build.random.RandomBuildSDK;
import dev.identra.app.x.hook.interceptors.build.random.RandomBuildTags;
import dev.identra.app.x.hook.interceptors.build.random.RandomBuildType;
import dev.identra.app.x.hook.interceptors.build.random.RandomBuildUser;
import dev.identra.app.x.hook.interceptors.build.random.RandomBuildVersion;
import dev.identra.app.x.hook.interceptors.build.random.RandomDevCodeName;
import dev.identra.app.x.hook.interceptors.build.random.RandomRomBootState;
import dev.identra.app.x.hook.interceptors.build.random.RandomRomName;
import dev.identra.app.x.hook.interceptors.build.random.RandomRomSecure;
import dev.identra.app.x.hook.interceptors.hardware.battery.random.RandomBatteryPercentLeft;
import dev.identra.app.x.hook.interceptors.hardware.battery.random.RandomBatteryTimeRemaining;
import dev.identra.app.x.hook.interceptors.hardware.bluetooth.random.RandomBluetoothName;
import dev.identra.app.x.hook.interceptors.hardware.bluetooth.random.RandomBluetoothSOC;
import dev.identra.app.x.hook.interceptors.hardware.bluetooth.random.RandomBluetoothState;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomAbiList;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomAbiList32;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomAbiList64;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomCpuAbi;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomCpuArch;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomCpuBaseBand;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomCpuHardwareName;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomCpuId;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomCpuManufacturer;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomCpuModel;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomCpuPlatformName;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomCpuProcessorCount;
import dev.identra.app.x.hook.interceptors.hardware.cpu.random.RandomCpuVmVar;
import dev.identra.app.x.hook.interceptors.hardware.kernel.random.RandomKernelNodeName;
import dev.identra.app.x.hook.interceptors.hardware.kernel.random.RandomKernelRelease;
import dev.identra.app.x.hook.interceptors.hardware.kernel.random.RandomKernelSysName;
import dev.identra.app.x.hook.interceptors.hardware.kernel.random.RandomKernelVersion;
import dev.identra.app.x.hook.interceptors.zone.random.RandomDateFour;
import dev.identra.app.x.hook.interceptors.zone.random.RandomDateOne;
import dev.identra.app.x.hook.interceptors.zone.random.RandomDateThree;
import dev.identra.app.x.hook.interceptors.zone.random.RandomDateTwo;
import dev.identra.app.x.hook.interceptors.zone.random.RandomDateZero;
import dev.identra.app.x.core.settings.deprecated.SettingExtendedOld;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.randomizers.RandomGenericStringOne;
import dev.identra.app.x.core.settings.random_old.randomizers.RandomGenericTrueFalse;
import dev.identra.app.x.core.settings.random_old.randomizers.RandomGenericTrueFalseNumber;
import dev.identra.app.x.core.settings.random_old.randomizers.RandomUserSerial;
import dev.identra.app.x.core.settings.random_old.randomizers.etc.RandomEtcNetflix;

public class RandomHelper {
    private static final String TAG = "Identra.RandomHelper";
    private static final Map<String, IRandomizer> randomizers = new HashMap<>();

    //public static boolean isValid(IRandomizer randomizer) { return randomizer != null && !TextUtils.isEmpty(randomizer.getDisplayName()) && randomizer.hasSettings() && !randomizer.isOptionFiller(); }


    private static void pushRandomizer(IRandomizer randomizer) {
        //if(!isValid(randomizer)) {
        //    Log.w(TAG, "Randomizer is Not Valid not putting into Global List: " + Str.toStringOrNull(randomizer));
        //    return;
        //}
        //for (String s : randomizer.getSettings())  randomizers.put(s, randomizer);
    }

    private static void internalInitGenericRandomizers() {
        pushRandomizer(RandomGenericStringOne.create());
        pushRandomizer(RandomGenericTrueFalse.create());
        pushRandomizer(RandomGenericTrueFalseNumber.create());
    }

    private static void internalInitEtcRandomizers() {
        pushRandomizer(RandomEtcNetflix.create());
    }

    private static void internalInitCpuRandomizers() {
        pushRandomizer(RandomAbiList.create());
        pushRandomizer(RandomAbiList32.create());
        pushRandomizer(RandomAbiList64.create());
        pushRandomizer(RandomCpuAbi.create());          //Parent
        pushRandomizer(RandomCpuArch.create());
        pushRandomizer(RandomCpuBaseBand.create());
        pushRandomizer(RandomCpuId.create());
        pushRandomizer(RandomCpuVmVar.create());
        pushRandomizer(RandomCpuHardwareName.create());
        pushRandomizer(RandomCpuPlatformName.create());
        pushRandomizer(RandomCpuProcessorCount.create());
        pushRandomizer(RandomCpuManufacturer.create());
        pushRandomizer(RandomCpuModel.create());
    }

    private static void internalInitROMRandomizers() {
        pushRandomizer(RandomRomBootState.create());
        pushRandomizer(RandomRomName.create());
        pushRandomizer(RandomRomSecure.create());
    }

    private static void internalInitBluetoothRandomizers() {
        pushRandomizer(RandomBluetoothName.create());
        pushRandomizer(RandomBluetoothSOC.create());
        pushRandomizer(RandomBluetoothState.create());
    }

    private static void internalInitBatteryRandomizers() {
        pushRandomizer(RandomBatteryPercentLeft.create());
        pushRandomizer(RandomBatteryTimeRemaining.create());
    }

    private static void internalInitKernelRandomizers() {
        pushRandomizer(RandomKernelVersion.create());
        pushRandomizer(RandomKernelRelease.create());
        pushRandomizer(RandomKernelNodeName.create());
        pushRandomizer(RandomKernelSysName.create());
    }

    private static void internalInitGenericUserRandomizers() {
        //pushRandomizer(RandomUserName.create());
        pushRandomizer(RandomUserSerial.create());
    }

    private static void internalInitBuildRandomizers() {
        pushRandomizer(RandomBaseOs.create());
        pushRandomizer(RandomBuildCodeName.create());
        pushRandomizer(RandomRomName.create());
        pushRandomizer(RandomBuildId.create());
        pushRandomizer(RandomBuildRadio.create());
        pushRandomizer(RandomBuildSDK.create());
        pushRandomizer(RandomBuildTags.create());
        pushRandomizer(RandomBuildType.create());
        pushRandomizer(RandomBuildUser.create());
        pushRandomizer(RandomBuildVersion.create());
        pushRandomizer(RandomDevCodeName.create());
    }

    private static void internalInitDateRandomizers() {
        pushRandomizer(RandomDateZero.create());
        pushRandomizer(RandomDateOne.create());
        pushRandomizer(RandomDateTwo.create());
        pushRandomizer(RandomDateThree.create());
        pushRandomizer(RandomDateFour.create());
    }

    private static void internalInitNetworkRandomizers() {
        /*pushRandomizer(RandomDNSList.create());
        pushRandomizer(RandomDHCPServer.create());
        pushRandomizer(RandomDNS.create());
        pushRandomizer(RandomGateway.create());
        pushRandomizer(RandomHostName.create());
        pushRandomizer(RandomNetAddress.create());
        pushRandomizer(RandomNetMask.create());
        pushRandomizer(RandomRoutes.create());
        pushRandomizer(RandomSSID.create());*/
    }

    private static void internalInitHardwareRandomizers() {
        /*pushRandomizer(RandomGpsModelName.create());
        pushRandomizer(RandomGpsModelYear.create());
        pushRandomizer(RandomCameraCount.create());*/
    }

    private static void internalInitCellRandomizers() {
        //pushRandomizer(RandomMCC.create());
        //pushRandomizer(RandomMNC.create());
    }

    public static IRandomizer getRandomizer(String settingName) {
        synchronized (randomizers) {
            if(randomizers.isEmpty()) {
                internalInitEtcRandomizers();
                internalInitGenericRandomizers();

                internalInitCpuRandomizers();
                internalInitBluetoothRandomizers();
                internalInitROMRandomizers();
                internalInitBatteryRandomizers();
                internalInitKernelRandomizers();
                internalInitGenericUserRandomizers();
                internalInitBuildRandomizers();
                internalInitDateRandomizers();
                internalInitNetworkRandomizers();
                internalInitCellRandomizers();
                internalInitHardwareRandomizers();
            }

            return randomizers.get(settingName.toLowerCase());
        }
    }

    public static void bindRandomizer(SettingExtendedOld setting) {
        if(setting != null) {
            setting.bindRandomizer(getRandomizer(setting.getNameWithoutIndex()));
        }
    }
}
