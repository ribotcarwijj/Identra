package dev.identra.xposed;

import java.lang.reflect.Member;

/**
 * Identra compat layer: legacy-style hook callback, executed on top of the
 * libxposed API 102 interceptor chain (see XposedBridge). No de.robv code.
 */
public abstract class XC_MethodHook {
    public XC_MethodHook() { }

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable { }

    protected void afterHookedMethod(MethodHookParam param) throws Throwable { }

    public static final class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;
        private Object result = null;
        private Throwable throwable = null;
        boolean returnEarly = false;

        public Object getResult() { return result; }

        public void setResult(Object result) {
            this.result = result;
            this.throwable = null;
            this.returnEarly = true;
        }

        public Throwable getThrowable() { return throwable; }

        public boolean hasThrowable() { return throwable != null; }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
            this.result = null;
            this.returnEarly = true;
        }

        void setResultSilently(Object r) { this.result = r; this.throwable = null; }

        void setThrowableSilently(Throwable t) { this.throwable = t; this.result = null; }

        public Object getResultOrThrowable() throws Throwable {
            if (throwable != null) throw throwable;
            return result;
        }
    }

    public final class Unhook {
        private final io.github.libxposed.api.XposedInterface.HookHandle handle;

        Unhook(io.github.libxposed.api.XposedInterface.HookHandle handle) { this.handle = handle; }

        public Member getHookedMethod() { return handle.getExecutable(); }

        public XC_MethodHook getCallback() { return XC_MethodHook.this; }

        public void unhook() { handle.unhook(); }
    }
}
