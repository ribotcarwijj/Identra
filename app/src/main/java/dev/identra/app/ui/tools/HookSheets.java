package dev.identra.app.ui.tools;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collections;

import dev.identra.app.R;
import dev.identra.app.ui.IdentraAppPicker;
import dev.identra.app.ui.IdentraData;
import dev.identra.app.ui.IdentraSheet;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.commands.call.PutHookExCommand;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.ui.adapters.hooks.elements.ResultRequest;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;

/**
 * 0.5.0: the hook detail sheet shared by the Hooks browser and the Hook editor. Shows the whole
 * definition (class, method, parameters, values it reads, Lua script read-only in monospace) and
 * the actions: turn on/off for one app, enable/disable everywhere (the definition's enabled flag,
 * saved through putHookExCommand like every edit), duplicate as a custom hook, and edit/delete
 * for custom hooks. Built-in definitions are never deleted.
 */
public final class HookSheets {
    private static final String TAG = "Identra.UiHookSheet";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private HookSheets() { }

    public interface Changed { void onChanged(); }

    private static String nz(String s) { return s == null ? "" : s; }

    public static void details(final Activity a, final XHook h, final Changed changed) {
        IdentraSheet s = new IdentraSheet(a).title(nz(h.name)).sub(IdentraData.groupTitle(a, h.group));
        s.field(a.getString(R.string.identra_hook_group), h.group);
        s.field(a.getString(R.string.identra_hook_description), h.description);
        s.field(a.getString(R.string.identra_hook_class), h.getClassName());
        s.field(a.getString(R.string.identra_hook_method), h.methodName);
        if (h.hasParameters()) s.field(a.getString(R.string.identra_hook_params), Str.joinList(h.parameterTypes, ", "));
        s.field(a.getString(R.string.identra_hook_return), h.returnType);
        s.field(a.getString(R.string.identra_hook_sdk), h.minSdk + " – " + (h.maxSdk >= 999 ? "∞" : String.valueOf(h.maxSdk)));
        if (h.hasSettings()) s.field(a.getString(R.string.identra_hook_settings), Str.joinList(h.settings, "\n"));
        if (!h.targetPackages.isEmpty()) s.field(a.getString(R.string.identra_hook_targets), Str.joinList(h.targetPackages, "\n"));
        if (!h.excludePackages.isEmpty()) s.field(a.getString(R.string.identra_hook_excludes), Str.joinList(h.excludePackages, "\n"));
        StringBuilder flags = new StringBuilder();
        flags.append(h.isBuiltIn() ? a.getString(R.string.identra_hook_builtin) : a.getString(R.string.identra_hooks_custom));
        boolean disabled = h.enabled != null && !h.enabled;
        if (disabled) flags.append(" · ").append(a.getString(R.string.identra_hooks_disabled));
        if (Boolean.TRUE.equals(h.optional)) flags.append(" · ").append(a.getString(R.string.identra_hook_optional));
        if (Boolean.TRUE.equals(h.notify)) flags.append(" · ").append(a.getString(R.string.identra_hook_notify));
        s.field(a.getString(R.string.identra_hook_flags), flags);
        s.mono(a.getString(R.string.identra_hook_script), h.luaScript);

        s.action(IdentraSheet.STYLE_FILLED, a.getString(R.string.identra_hookedit_on_for_app), v ->
                IdentraAppPicker.show(a, a.getString(R.string.identra_hookedit_on_for_app), app -> {
                    boolean started = IdentraData.assign(a, app, Collections.singletonList(h), code -> report(a, code,
                            a.getString(R.string.identra_hookedit_on_done, IdentraData.labelOf(app))));
                    if (!started) toast(a, a.getString(R.string.identra_hookedit_already_on, IdentraData.labelOf(app)));
                }), true);
        s.action(IdentraSheet.STYLE_TONAL, a.getString(R.string.identra_hookedit_off_for_app), v ->
                IdentraAppPicker.show(a, a.getString(R.string.identra_hookedit_off_for_app), app -> {
                    boolean started = IdentraData.unassign(a, app, Collections.singletonList(h.getObjectId()), code -> report(a, code,
                            a.getString(R.string.identra_hookedit_off_done, IdentraData.labelOf(app))));
                    if (!started) toast(a, a.getString(R.string.identra_hookedit_already_off, IdentraData.labelOf(app)));
                }), true);
        s.action(IdentraSheet.STYLE_OUTLINED, a.getString(disabled ? R.string.identra_hookedit_enable_all : R.string.identra_hookedit_disable_all), v -> {
            XHook copy = XHook.copy(h);
            copy.enabled = disabled;
            save(a, copy, false, changed);
        }, true);
        s.action(IdentraSheet.STYLE_OUTLINED, a.getString(R.string.identra_hookedit_duplicate), v ->
                a.startActivity(IdentraHookEditorActivity.duplicate(a, h)), true);
        if (!h.isBuiltIn()) {
            s.action(IdentraSheet.STYLE_OUTLINED, a.getString(R.string.identra_hookedit_edit), v ->
                    a.startActivity(IdentraHookEditorActivity.edit(a, h)), true);
            s.action(IdentraSheet.STYLE_TEXT, a.getString(R.string.identra_hookedit_delete), v ->
                    new MaterialAlertDialogBuilder(a)
                            .setTitle(R.string.identra_hookedit_delete_title)
                            .setMessage(a.getString(R.string.identra_hookedit_delete_msg, nz(h.name)))
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.identra_hookedit_delete, (d, w) -> save(a, XHook.copy(h), true, changed))
                            .show(), true);
        }
        s.show();
    }

    /** Saves (or deletes) a definition off the main thread; reports on the main thread. */
    public static void save(final Activity a, final XHook hook, final boolean delete, final Changed changed) {
        final Context app = a.getApplicationContext();
        IdentraData.EXECUTOR.submit(() -> {
            ResultRequest res;
            try {
                res = PutHookExCommand.putEx(app, hook, delete);
            } catch (Throwable t) {
                Log.e(TAG, "Save failed: " + t);
                res = null;
            }
            final boolean ok = res != null && res.successful();
            final String err = res == null ? "?" : res.exception;
            MAIN.post(() -> {
                if (a.isFinishing()) return;
                if (ok) {
                    toast(a, a.getString(delete ? R.string.identra_hookedit_deleted : R.string.identra_hookedit_saved, nz(hook.name)));
                    if (changed != null) changed.onChanged();
                } else {
                    toast(a, a.getString(R.string.identra_change_failed, nz(err)));
                }
            });
        });
    }

    private static void report(final Activity a, final A_CODE code, final String okText) {
        MAIN.post(() -> {
            if (a.isFinishing()) return;
            toast(a, A_CODE.isSuccessful(code) ? okText : a.getString(R.string.identra_change_failed, String.valueOf(code)));
        });
    }

    private static void toast(Context c, String text) { Toast.makeText(c, text, Toast.LENGTH_LONG).show(); }
}
