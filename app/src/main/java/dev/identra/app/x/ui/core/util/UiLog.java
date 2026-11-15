package dev.identra.app.x.ui.core.util;

import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import dev.identra.app.R;
import dev.identra.app.x.runtime.RuntimeUtils;
import dev.identra.app.x.ui.core.UserClientAppContext;
import dev.identra.app.x.ui.dialogs.utils.DialogUtils;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.settings.SettingsContainer;

public class UiLog {
    private static final String TAG = LibUtil.generateTag(UiLog.class);


    public static boolean ensureNotGlobal(UserClientAppContext appContext) { return ensureNotGlobal(null, appContext); }
    public static boolean ensureNotGlobal(View view, UserClientAppContext appContext) {
        if(appContext == null) {
            DialogUtils.snack_bar(view, R.string.msg_error_generic_null);
            Log.e(TAG, "[ensureNotGlobal] App Context is Null! Stack=" + RuntimeUtils.getStackTraceSafeString(new Exception()));
            return false;
        }

        if(appContext.isGlobal()) {
            DialogUtils.snack_bar(view, R.string.msg_error_global_limit);
            return false;
        }

        return true;
    }

    public static boolean ensureHasDataContainer(SettingsContainer container) { return ensureHasDataContainer(null, container); }
    public static boolean ensureHasDataContainer(View view, SettingsContainer container) {
        if(container == null) {
            if(view != null)
                Snackbar.make(view, view.getResources().getString(R.string.msg_error_generic_null), Snackbar.LENGTH_LONG).show();

            Log.e(TAG, "[ensureHasDataContainer] Setting Container is Null! Stack=" + RuntimeUtils.getStackTraceSafeString());
            return true;
        }


        if(container.data.total < 1) {
            if(view != null)
                Snackbar.make(view, view.getResources().getString(R.string.msg_error_no_hooks), Snackbar.LENGTH_LONG).show();

            return true;
        }

        return false;
    }
}
