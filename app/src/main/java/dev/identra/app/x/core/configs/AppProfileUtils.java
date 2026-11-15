package dev.identra.app.x.core.configs;

import android.os.Environment;
import android.util.Log;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;

import dev.identra.app.DebugUtil;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.string.StrBuilder;
import dev.identra.app.x.data.utils.ArrayUtils;
import dev.identra.app.x.file.FileApi;
import dev.identra.app.x.file.FileEx;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.database.sql.SQLDatabase;

public class AppProfileUtils {
    private static final String TAG = "Identra.AppProfileUtils";

    public static final String DIR_DATA_DATA = FileApi.buildPath(Environment.getDataDirectory().getAbsolutePath(), "data");
    public static final String DIR_SOURCE = FileApi.buildPath(Environment.getDataDirectory().getAbsolutePath(), "app");
    public static final String DIR_DATA = FileApi.buildPath(Environment.getDataDirectory().getAbsolutePath(), "user");                  //Append Profile ID
    public static final String DIR_PROTECTED_DATA = FileApi.buildPath(Environment.getDataDirectory().getAbsolutePath(), "user_de");     //Append Profile ID
    public static final String DIR_EXTERNAL = FileApi.buildPath("storage", "emulated");

    public static final String DIR_TAG_SOURCE = "Source";
    public static final String DIR_TAG_DATA = "Data";
    public static final String DIR_TAG_DATA_DATA = "DataData";
    public static final String DIR_TAG_PROTECTED = "ProtectedData";
    public static final String DIR_TAG_EXTERNAL = "External";

    public static List<PathDetails> getAppDirectories(int userId, String packageName) {
        String userString = String.valueOf(userId);
        if(DebugUtil.isDebug())
            Log.d(TAG, "Getting Directories for App [" + packageName + "] UserId [" + userId + "]");

        List<PathDetails> directories = new ArrayList<>();

        FileEx dFile = FileEx.createFromDirectory(DIR_SOURCE);
        FileEx dSource = FileApi.findFirst(dFile, packageName, true, false, true);
        if(dSource != null)
            directories.add(PathDetails.create(DIR_TAG_SOURCE, dSource.getAbsolutePath()));

        FileEx dDir = FileEx.createFromDirectory(FileApi.buildPath(DIR_DATA, userString, packageName));
        if(dDir.isDirectory())
            directories.add(PathDetails.create(DIR_TAG_DATA, dDir.getAbsolutePath()));

        FileEx pdDir = FileEx.createFromDirectory(FileApi.buildPath(DIR_PROTECTED_DATA, userString, packageName));
        if(pdDir.isDirectory())
            directories.add(PathDetails.create(DIR_TAG_PROTECTED, pdDir.getAbsolutePath()));

        FileEx exDir = FileEx.createFromDirectory(FileApi.buildPath(DIR_EXTERNAL, userString, "Android", "data", packageName));
        if(exDir.isDirectory())
            directories.add(PathDetails.create(DIR_TAG_EXTERNAL, exDir.getAbsolutePath()));

        FileEx ddDir = FileEx.createFromDirectory(FileApi.buildPath(DIR_DATA_DATA, packageName));
        if(exDir.isDirectory())
            directories.add(PathDetails.create(DIR_TAG_DATA_DATA, ddDir.getAbsolutePath()));

        if(DebugUtil.isDebug())
            Log.d(TAG, "Dirs Pkg=" + packageName + " Data=" + dump(directories));

        return directories;
    }


    //public static List<String> backupDirectories(String profileName, List<String> backupDirectories) {
    //}



    public static List<String> restorePaths(int userId, String packageName, String profileName) {
        List<String> restored = new ArrayList<>();
        try {


        }catch (Exception e) {
            Log.e(TAG, "Failed to Restore Directories! ");
        }

        return null;
    }



    public static FileEx resolveDataApp(String packageName) {
        try {
            FileEx baseDir = new FileEx(DIR_SOURCE);
            if(!baseDir.isDirectory())
                throw new FileNotFoundException(baseDir.getAbsolutePath());

            List<FileEx> found = FileApi.find(baseDir, packageName, true, false, true, true);
            if(found.isEmpty())
                throw new FileNotFoundException("Failed to Find Package Name App Dir: " + packageName + " Empty Search Result");

            FileEx file = found.iterator().next();
            if(DebugUtil.isDebug())
                Log.d(TAG, "Found APKs App Directory, Pkg=" + packageName + " Path=" + file.getAbsolutePath());

            return file;
        }catch (Exception e) {
            Log.e(TAG, "Failed to Resolve /data/app Path for Package=" + packageName + " Error=" + e);
            return null;
        }
    }

    public static String dump(List<PathDetails> details) {
        StrBuilder sb = new StrBuilder();
        sb.append("\n");
        int i = 0;
        for(PathDetails d : details) {
            sb.appendDividerTitleLine(String.valueOf(i));
            sb.appendFieldLine("Tag", d.tag);
            sb.appendFieldLine("Path", d.fullPath);
            i++;
        }

        return sb.toString(true);
    }
}
