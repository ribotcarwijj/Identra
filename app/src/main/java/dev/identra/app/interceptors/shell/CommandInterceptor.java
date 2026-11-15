package dev.identra.app.interceptors.shell;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;

import dev.identra.app.DebugUtil;
import dev.identra.app.x.Str;
import dev.identra.app.api.xstandard.interfaces.ICommandIntercept;
import dev.identra.app.interceptors.UserContextMaps;
import dev.identra.app.utilities.StringUtil;

public abstract class CommandInterceptor implements ICommandIntercept {
    private static final String TAG = "Identra.ShellIntercept.CommandInterceptor";
    protected String command;
    protected String setting;
    public CommandInterceptor() { }

    @Override
    public  boolean keepGoing(UserContextMaps maps, String key) {
        boolean isEnabled = StringUtil.toBoolean(maps.getSetting(key, "false"), false);
        if(DebugUtil.isDebug())
            Log.d(TAG, "Is Interceptor Enabled ? => " + (isEnabled) + " Setting Name=" + key);

        return isEnabled;
    }
    @Override
    public boolean interceptCommand(ShellInterception result) { return false; }
    @Override
    public String getCommand() { return this.command; }

    public String getSetting() { return this.setting; }

    @Override
    public boolean containsCommand(String input) { return input != null && input.toLowerCase().contains(command.toLowerCase()); }

    @Override
    public boolean isCommand(ShellInterception results) {
        //if(results.hasCommand(getCommand()))
        return results.hasCommand(getCommand());
    }

    @Override
    public boolean containsCommand(List<String> commands) {
        for(String s : commands) {
            String sLow = s.toLowerCase().trim();
            if(sLow.contains(this.command)) {
                if(DebugUtil.isDebug())
                    Log.d(TAG, "Command is Found from the Given Target Commands => " + Str.joinList(commands, " "));
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() { return this.command; }
}
