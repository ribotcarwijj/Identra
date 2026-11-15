package dev.identra.app.interceptors.shell.handlers;

import java.util.UUID;

import dev.identra.app.api.xstandard.interfaces.ICommandIntercept;
import dev.identra.app.interceptors.UserContextMaps;
import dev.identra.app.interceptors.shell.CommandInterceptor;
import dev.identra.app.interceptors.shell.ShellInterception;
import dev.identra.app.x.hook.interceptors.file.FileInterceptor;

public class CatIDIntercept extends CommandInterceptor implements ICommandIntercept {
    private static final String CAT_SERIAL_ID_SETTING = "intercept.shell.serial.bool";

    @SuppressWarnings("unused")
    public CatIDIntercept() {
        this.command = "serial";
        this.setting = CAT_SERIAL_ID_SETTING;
    }

    @Override
    public boolean isCommand(ShellInterception results) {
        FileInterceptor.ensureInit(results.param);
        boolean foundCat = false;
        for (String command : results.commandLine) {
            if(!foundCat) {
                if (command.equalsIgnoreCase("cat"))
                    foundCat = true;
            } else {
                if(command.contains("/") && !FileInterceptor.isAllowed(command)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean interceptCommand(ShellInterception result) {
        if (result != null && result.isValid) {
            boolean foundCat = false;
            for (String command : result.commandLine) {
                if(!foundCat) {
                    if (command.equalsIgnoreCase("cat"))
                        foundCat = true;
                } else {
                    if(command.contains("/") && !FileInterceptor.isAllowed(command)) {
                        result.setIsMalicious(true);
                        result.setNewValue("cat: " + command + ": No such file or directory");
                        return true;
                    }
                }
            }
        }
        return false;
    }
}