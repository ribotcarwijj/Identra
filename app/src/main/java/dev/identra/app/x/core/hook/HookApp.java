package dev.identra.app.x.core.hook;

import androidx.annotation.NonNull;

import dev.identra.app.x.Str;
import dev.identra.app.x.ui.core.UserClientAppContext;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.identity.UserIdentityIO;
import dev.identra.app.x.core.identity.UserIdentityUtils;

public class HookApp {
    public static HookApp create(UserClientAppContext app) { return new HookApp(app.appUid, app.appPackageName); }
    public static HookApp create(int uid, String packageName) { return new HookApp(uid, packageName); }

    public final int uid;
    public final String packageName;
    public int getUserId() { return UserIdentityUtils.getUserId(uid); }
    public boolean isGlobal() { return UserIdentity.GLOBAL_NAMESPACE.equalsIgnoreCase(packageName); }

    public HookApp(int uid, String packageName) {
        this.uid = uid;
        this.packageName = packageName;
    }

    public static boolean isGlobalApp(HookApp app) { return app == null || app.isGlobal(); }

    @NonNull
    @Override
    public String toString() {
        return "Uid=" + uid + " Pkg=" + packageName;
    }
}
