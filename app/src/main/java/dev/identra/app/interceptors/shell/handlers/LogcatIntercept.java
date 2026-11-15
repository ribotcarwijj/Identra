package dev.identra.app.interceptors.shell.handlers;

import android.util.Log;

import java.util.List;

import dev.identra.app.BuildConfig;
import dev.identra.app.api.xstandard.interfaces.ICommandIntercept;
import dev.identra.app.interceptors.UserContextMaps;
import dev.identra.app.interceptors.shell.CommandInterceptor;
import dev.identra.app.interceptors.shell.ShellInterception;
import dev.identra.app.utilities.CollectionUtil;
import dev.identra.app.utilities.StringUtil;

public class LogcatIntercept extends CommandInterceptor implements ICommandIntercept {
    private static final String TAG = "Identra.LogcatIntercept";

    private static final String LOGCAT_INTERCEPT_SETTING = "intercept.shell.logcat.bool";

    @SuppressWarnings("unused")
    public LogcatIntercept() { this.command = "logcat"; }

    @Override
    public boolean interceptCommand(ShellInterception result) {
        if(result != null && result.isValid) {
            UserContextMaps maps = result.getUserMaps();
            if(maps != null) {
                String low = result.getCommandLine().toLowerCase().trim();
                if(!StringUtil.isValidString(low)) {
                    Log.e(TAG, "Some how the String low is null or empty...");
                    return false;
                }

                List<String> parts = StringUtil.breakStringExtreme(low);
                if(CollectionUtil.isEmptyValuesOrInvalid(parts)) {
                    Log.e(TAG, "String List after low broken is NULL or Empty...");
                    return false;
                }

                if(parts.get(0).equalsIgnoreCase(this.command)) {
                    if(BuildConfig.DEBUG)
                        Log.w(TAG, "(" + this.command + ")" + " command was found...");

                    if(!keepGoing(maps, LOGCAT_INTERCEPT_SETTING)) return true;
                    result.setNewValue("");
                    result.setIsMalicious(true);
                    if(BuildConfig.DEBUG)
                        Log.i(TAG, "blocking (" + this.command + ") command from executing...");

                    return true;
                }
            }
        } return false;
    }
}
