package dev.identra.app.x.hook.filter;

import java.util.HashMap;

import dev.identra.app.api.hook.IdentraHook;

//FilterFactory ?
public class FilterHook extends IdentraHook {
    public static final String DEFAULT_COLLECTION = "Identra";

    public static FilterHook create(String filterKind) { return new FilterHook(filterKind); }
    public static FilterHook create(String filterKind, String collection) { return new FilterHook(filterKind, collection); }

    public String filterKind;
    public String collection;
    public HashMap<String, String> methods = new HashMap<>();

    public FilterHook(String filterKind) { this(filterKind, DEFAULT_COLLECTION); }
    public FilterHook(String filterKind, String collection) {
        this.filterKind = filterKind;
        this.collection = collection;
    }

    public FilterHook bindMethod(Class<?> clazz, String methodName) { return bindMethod(clazz.getName(), methodName); }
    public FilterHook bindMethod(String className, String methodName) {
        methods.put(className, methodName);
        return this;
    }

    public boolean isFilterOrInterceptHook(IdentraHook hook) {
        return false;
    }
}
