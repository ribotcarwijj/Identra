package dev.identra.app.x.core.hook;

import java.util.Arrays;
import java.util.List;

import dev.identra.app.x.Str;

public class AssignmentUtils {
    //PS We can add Client Side Interception ?
    //This can block non cleaned cache of items being sent to client that they should not be using unless they think they are smart
    //Eventually migrate these to JSON, and the MAIN JSONs move them to a sub folder away from the filth
    //We can also maybe, make separate commands instead of handing off shit poor args in one com

    public static final List<String> BAD_ASSIGNMENTS = Arrays.asList(
            "Identra.Filter/Settings$System.getString",
            "Identra.Filter/Settings$Secure.getString",
            "Identra.Filter/Settings$Global.getString",
            "Identra.Filter/ContentResolver/Settings",
            "Identra.Filter/ContentResolver(uri)/Settings",

            "Identra.BlockGuardOs.open/Filter/Filter",
            "Identra.File.canRead/Filter",
            "Identra.File.canWrite/Filter",
            "Identra.File.canExecute/Filter",
            "Identra.File.exists/Filter",
            "Identra.File.isFile/Filter",
            "Identra.File.isDirectory/Filter",
            "Identra.File.list/Filter",
            "Identra.File.list/FileNameFilter/Filter",
            "Identra.File.listFiles/Filter",
            "Identra.File.listFiles/FileNameFilter/Filter",
            "Identra.File.listFiles/FileFilter/Filter",
            "Identra.File.listRoots/Filter",

            "Identra.Filter/ContentResolver.query16",
            "Identra.Filter/ContentResolver.query26",
            "Identra.Filter/ContentResolver.query1",

            "Identra.Intercept.Runtime.exec(command)",
            "Identra.Intercept.Runtime.exec(command, envp)",
            "Identra.Intercept.Runtime.exec(command, envp, dir)",
            "Identra.Intercept.Runtime.exec(commands)",
            "Identra.Intercept.Runtime.exec(commands, envp)",
            "Identra.Intercept.Runtime.exec(commands, envp, dir)",
            "Identra.Intercept.ProcessBuilder.start",

            "Identra.Intercept.System.getProperty(string)",
            "Identra.Intercept.System.getProperty(string, string)",
            "Identra.Intercept.SystemProperties.get(string)",
            "Identra.Intercept.SystemProperties.get(string, string)",

            "Identra.Filter/BinderProxy.transact(int, Parcel, Parcel, int)",
            "Identra.Filter/Binder.onTransact(*)",
            "Identra.Filter/Binder.execTransact(*)",
            "Identra.Filter/Binder.execTransactInternal(*)",
            "Identra.Filter/Binder.execTransactInternal(int, Parcel, Parcel, int, int)",
            "Identra.Filter/Binder.execTransact(int, Parcel, Parcel, int)"
    );

    public static boolean isSpecialSetting(String name) { return !Str.isEmpty(name) && name.toLowerCase().contains("setting:"); }
    public static boolean isFilterHook(String name) { return !Str.isEmpty(name) && BAD_ASSIGNMENTS.contains(name); }
}
