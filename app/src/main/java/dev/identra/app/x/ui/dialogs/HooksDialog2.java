package dev.identra.app.x.ui.dialogs;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.identra.app.DebugUtil;
import dev.identra.app.R;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.data.utils.TryRun;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.ui.core.dialog.CheckableDialog;
import dev.identra.app.x.ui.core.view_registry.SharedRegistry;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.call.AssignHooksCommand;
import dev.identra.app.x.core.commands.call.GetAppInfoCommand;
import dev.identra.app.x.core.commands.query.GetHooksCommand;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.core.hook.AssignmentsPacket;
import dev.identra.app.x.core.hook.HookGroupOrganizer;

public class HooksDialog2 extends CheckableDialog<XHook> {
    private static final String TAG = LibUtil.generateTag(HooksDialog2.class);

    public static HooksDialog2 create() { return new HooksDialog2(); }

    public interface IPositiveFinished { void onPositive(List<XHook> enabled, List<XHook> disabled); }

    private IPositiveFinished onPositive;

    public List<XHook> getItems() { return items; }

    public List<XHook> getEnabled() {
        List<XHook> enabled = new ArrayList<>();
        for(XHook hook : items) {
            if(this.viewRegistry != null && this.viewRegistry.isChecked(SharedRegistry.STATE_TAG_HOOKS, hook.getObjectId()))
                enabled.add(hook);
        }

        return enabled;
    }

    @Override
    protected void onFinishedPush(List<XHook> enabled, List<XHook> disabled) {
        TryRun.onMain(() -> {
            Log.d(TAG, Str.fm("Enabled=%s Disabled=%s Items=%s",
                    ListUtil.size(enabled),
                    ListUtil.size(disabled),
                    items.size()));

            if(this.onPositive != null)
                this.onPositive.onPositive(enabled, disabled);
        });
    }

    public HooksDialog2 setEvent(IPositiveFinished onPositive) {
        this.onPositive = onPositive;
        return this;
    }

    public HooksDialog2 set(Collection<XHook> hooks, Context context) {
        this.viewRegistry = new SharedRegistry();
        this.TAG_ITEMS = SharedRegistry.STATE_TAG_HOOKS;
        this.title = context.getString(R.string.menu_filter_hooks);
        this.useOriginalState = false;
        this.items.clear();
        if(ListUtil.isValid(hooks)) {
            for(XHook hook : hooks) {
                this.viewRegistry.setChecked(SharedRegistry.STATE_TAG_HOOKS, hook.getObjectId(), true);
                this.items.add(hook);
            }
        }

        return this;
    }
}
