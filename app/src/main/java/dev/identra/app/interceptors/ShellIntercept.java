package dev.identra.app.interceptors;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dev.identra.app.BuildConfig;
import dev.identra.app.DebugUtil;
import dev.identra.app.interceptors.shell.CommandInterceptor;
import dev.identra.app.interceptors.shell.handlers.CatBootIDIntercept;
import dev.identra.app.interceptors.shell.handlers.CatIDIntercept;
import dev.identra.app.interceptors.shell.handlers.GetPropIntercept;
import dev.identra.app.interceptors.shell.ShellInterception;
import dev.identra.app.interceptors.shell.handlers.LSIntercept;
import dev.identra.app.interceptors.shell.handlers.LogcatIntercept;
import dev.identra.app.interceptors.shell.handlers.MemInfoIntercept;
import dev.identra.app.interceptors.shell.handlers.StatIntercept;
import dev.identra.app.interceptors.shell.handlers.SuIntercept;
import dev.identra.app.interceptors.shell.handlers.UnameIntercept;

public class ShellIntercept {
    private static final String TAG = "Identra.ShellIntercept";
    private static final List<CommandInterceptor> interceptors = new ArrayList<>();

    public static ShellInterception intercept(ShellInterception results) {
        try {
            if(results.isValid) {
                if(DebugUtil.isDebug())
                    Log.d(TAG, "Checking command: " + results.getCommandLine());

                for(CommandInterceptor interceptor : getInterceptors()) {
                    if(interceptor.isCommand(results)) {
                        if(interceptor.interceptCommand(results)) {
                            if(DebugUtil.isDebug())
                                Log.w(TAG, "Malicious command! " + results.getCommandLine());

                            return results;
                        }
                    } else {
                        if(DebugUtil.isDebug())
                            Log.d(TAG, "Command Rejected => " + interceptor.getCommand() + " Command Line Data=" + results.getCommandLine());
                    }
                }
            }
        }catch (Exception e) { Log.e(TAG, "Error with Interceptor Core: e=" + e); }
        return results;
    }

    public static List<CommandInterceptor> getInterceptors() {
        if(interceptors.isEmpty()) {
            interceptors.add(new GetPropIntercept());
            interceptors.add(new MemInfoIntercept());
            //interceptors.add(new SuIntercept());
            interceptors.add(new UnameIntercept());
            interceptors.add(new LogcatIntercept());
            interceptors.add(new StatIntercept());
            interceptors.add(new LSIntercept());
            interceptors.add(new CatBootIDIntercept());
            interceptors.add(new CatIDIntercept());
        }

        return interceptors;
    }
}
