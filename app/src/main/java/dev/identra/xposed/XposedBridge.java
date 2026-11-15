package dev.identra.xposed;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;

/**
 * Identra compat layer. Same surface the Identra code used from the legacy
 * bridge (log / hookMethod / hookAllMethods / hookAllConstructors), backed by
 * libxposed API 102. The framework is attached by dev.identra.app.XposedEntry.
 */
public final class XposedBridge {
    private static final String TAG = "Identra";
    private static volatile XposedInterface framework;

    private XposedBridge() { }

    public static void attach(XposedInterface xi) { framework = xi; }

    public static XposedInterface framework() { return framework; }

    public static void log(String text) {
        XposedInterface xi = framework;
        if (xi != null) xi.log(Log.INFO, TAG, String.valueOf(text));
        else Log.i(TAG, String.valueOf(text));
    }

    public static void log(Throwable t) {
        XposedInterface xi = framework;
        if (xi != null) xi.log(Log.ERROR, TAG, String.valueOf(t), t);
        else Log.e(TAG, String.valueOf(t), t);
    }

    public static XC_MethodHook.Unhook hookMethod(Member member, final XC_MethodHook callback) {
        XposedInterface xi = framework;
        if (xi == null) throw new IllegalStateException("Identra: framework not attached");
        if (!(member instanceof Executable))
            throw new IllegalArgumentException("Not a method/constructor: " + member);
        XposedInterface.HookHandle handle = xi.hook((Executable) member)
                .intercept(chain -> {
                    XC_MethodHook.MethodHookParam p = new XC_MethodHook.MethodHookParam();
                    p.method = chain.getExecutable();
                    p.thisObject = chain.getThisObject();
                    p.args = chain.getArgs().toArray();
                    try {
                        callback.beforeHookedMethod(p);
                    } catch (Throwable t) {
                        log(t);
                        p.returnEarly = false;
                        p.setResultSilently(null);
                    }
                    if (!p.returnEarly) {
                        try {
                            p.setResultSilently(chain.proceed(p.args));
                        } catch (Throwable t) {
                            p.setThrowableSilently(t);
                        }
                    }
                    Object lastResult = p.getResult();
                    Throwable lastThrowable = p.getThrowable();
                    try {
                        callback.afterHookedMethod(p);
                    } catch (Throwable t) {
                        log(t);
                        if (lastThrowable == null) p.setResultSilently(lastResult);
                        else p.setThrowableSilently(lastThrowable);
                    }
                    if (p.hasThrowable()) throw p.getThrowable();
                    return p.getResult();
                });
        return callback.new Unhook(handle);
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> clazz, String name, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> out = new HashSet<>();
        for (Method m : clazz.getDeclaredMethods())
            if (m.getName().equals(name) && !Modifier.isAbstract(m.getModifiers()))
                out.add(hookMethod(m, callback));
        return out;
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> clazz, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> out = new HashSet<>();
        for (Constructor<?> c : clazz.getDeclaredConstructors())
            out.add(hookMethod(c, callback));
        return out;
    }
}
