package dev.identra.app.x.hook;

import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.x.Str;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;

public class HookDefinitionAll extends HookDefinition {
    private String methodName = null;
    @Override
    protected String getName() { return
            !Str.isEmpty(methodName) ? methodName : blob != null ? blob.methodName : ""; }

    public HookDefinitionAll(Class<?> resolvedClazz, XHook blob) {
        this.blob = blob;
        this.resolvedClazz = resolvedClazz;
    }

    public HookDefinitionAll(Class<?> resolvedClazz, XHook blob, String methodName) {
        this.blob = blob;
        this.resolvedClazz = resolvedClazz;
        this.methodName = methodName;
    }
}
