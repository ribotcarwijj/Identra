package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.BuildConfig;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;

/**
 * 0.6.1: which Identra build runs in system_server (its versionCode). The command protocol
 * (getVersion = IdentraNs.PROTOCOL) stays backward compatible; this only tells the app whether the
 * system part is older than the installed APK, so it can hot-reload it. Builds before 0.6.1 do not
 * know the command and answer nothing (-1).
 */
public class GetBuildCommand extends CallCommandHandlerEx {
    public static final String COMMAND_NAME = "identraBuild";

    public GetBuildCommand() {
        name = COMMAND_NAME;
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket packet) {
        Bundle b = new Bundle();
        b.putInt("build", BuildConfig.VERSION_CODE);
        b.putString("name", BuildConfig.VERSION_NAME);
        return b;
    }

    /** versionCode of the system part, or -1 when it predates 0.6.1 / does not answer. */
    public static int get(Context context) {
        try {
            Bundle r = XProxyContent.luaCall(context, COMMAND_NAME);
            return r == null ? -1 : r.getInt("build", -1);
        } catch (Throwable t) {
            return -1;
        }
    }
}
