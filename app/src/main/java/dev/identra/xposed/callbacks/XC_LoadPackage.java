package dev.identra.xposed.callbacks;

import android.content.pm.ApplicationInfo;

/** Identra compat layer: legacy-shaped load-package parameters filled from libxposed. */
public final class XC_LoadPackage {
    private XC_LoadPackage() { }

    public static final class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public ApplicationInfo appInfo;
        public boolean isFirstApplication;
    }
}
