package dev.identra.app.x.hook.filter.kinds;

import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.interceptors.ShellIntercept;
import dev.identra.app.interceptors.shell.CommandInterceptor;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.TypeMap;
import dev.identra.app.x.hook.filter.FilterContainerElement;
import dev.identra.app.x.hook.filter.IFilterContainer;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;

public class ShellFilterContainer extends FilterContainerElement implements IFilterContainer {
    public static IFilterContainer create() { return new ShellFilterContainer(); }

    public static final String GROUP_NAME = "Intercept.Shell";
    public static final TypeMap DEFINITIONS = TypeMap.create().add(Runtime.class, "exec").add(ProcessBuilder.class, "start");

    public ShellFilterContainer() { super(GROUP_NAME, DEFINITIONS); }

    @Override
    public boolean hasSwallowedAsRule(XHook hook) {
        boolean isRule = super.hasSwallowedAsRule(hook);
        if(isRule) {
            String method = hook.methodName;
            for(CommandInterceptor interceptor : ShellIntercept.getInterceptors()) {
                if(interceptor.getCommand().toLowerCase().endsWith(method.toLowerCase())) {
                    createdSettings.put(interceptor.getSetting(), Str.TRUE);
                    break;
                }
            }
        }

        return isRule;
    }
}
