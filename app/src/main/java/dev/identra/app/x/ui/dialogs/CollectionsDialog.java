package dev.identra.app.x.ui.dialogs;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dev.identra.app.DebugUtil;
import dev.identra.app.R;
import dev.identra.app.x.Str;
import dev.identra.app.x.runtime.RuntimeUtils;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.ui.core.dialog.CheckableDialog;
import dev.identra.app.x.ui.core.view_registry.IIdentifiableObject;
import dev.identra.app.x.ui.core.view_registry.SharedRegistry;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.call.GetSettingExCommand;
import dev.identra.app.x.core.commands.call.PutSettingExCommand;
import dev.identra.app.x.core.commands.query.GetHooksCommand;
import dev.identra.app.x.core.database.ActionFlag;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.identity.UserIdentity;
import dev.identra.app.x.core.settings.data.SettingPacket;

public class CollectionsDialog extends CheckableDialog<CollectionsDialog.XCollection> {
    private static final String TAG = LibUtil.generateTag(CollectionsDialog.class);

    public static class XCollection implements IIdentifiableObject {
        public String id;
        @Override
        public String getObjectId() { return id; }

        @Override
        public void setId(String id) { this.id = id; }
    }

    public static interface OnDialogCloseListener {
        void onDialogClose();
    }

    @Override
    protected String getDisplayName(XCollection item) {
        return item.getObjectId();
    }

    private OnDialogCloseListener closeListener;
    public CollectionsDialog setOnDialogCloseListener(OnDialogCloseListener listener) {
        this.closeListener = listener;
        return this;
    }

    @Override
    protected void onFinishedPush(List<XCollection> enabled, List<XCollection> disabled) {
        StringBuilder sb = new StringBuilder();
        for(XCollection c : enabled) {
            if(sb.length() > 0)
                sb.append(",");

            sb.append(c.getObjectId());
        }

        if(DebugUtil.isDebug())
            Log.d(TAG, "Sending new Collections to Service! Collections=" + sb);

        SettingPacket packet = new SettingPacket(GetSettingExCommand.SETTING_COLLECTION, sb.toString());
        packet.setUserIdentity(UserIdentity.fromUid(Process.myUid(), UserIdentity.GLOBAL_NAMESPACE));
        packet.setActionPacket(ActionPacket.create(ActionFlag.PUSH, false));
        A_CODE code = PutSettingExCommand.call(context, packet);
        if(DebugUtil.isDebug())
            Log.d(TAG, "Send Collection Setting Packet, Code=" + code + " Packet=" + packet);

        try {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if(closeListener != null)
                        closeListener.onDialogClose();
                });
            } else {
                if(closeListener != null)
                    closeListener.onDialogClose();
            }
        }catch (Exception e) {
            Log.e(TAG, "Error Invoking end Event for Dialog Collection! Error=" + e + " Stack=" + RuntimeUtils.getStackTraceSafeString(e));
        }
    }

    public CollectionsDialog set(Context context) {
        if(context == null) return this;

        this.title = context.getString(R.string.menu_collections);
        this.useOriginalState = false;
        this.viewRegistry = new SharedRegistry();
        this.TAG_ITEMS = SharedRegistry.STATE_TAG_COLLECTIONS;

        List<XHook> allHooks =  GetHooksCommand.getHooks(context, true, true);
        List<String> collections = GetSettingExCommand.getCollections(context, Process.myUid());
        if(DebugUtil.isDebug())
            Log.d(TAG, Str.fm("Got Hooks Count=%s Collections=%s]", allHooks.size(), Str.joinList(collections)));

        List<String> added = new ArrayList<>();
        for(XHook hook : allHooks) {
            String collection = hook.collection;
            if(!TextUtils.isEmpty(collection)) {
                if(!added.contains(collection)) {
                    XCollection obj = new XCollection();
                    obj.id = collection;

                    items.add(obj);
                    added.add(collection);

                    boolean enabled = collections.contains(collection);
                    this.viewRegistry.setChecked(TAG_ITEMS, collection, enabled);
                    if(DebugUtil.isDebug())
                        Log.d(TAG, "Added Collection to List, Collection=" + collection + " Enabled=" + enabled);
                }
            }
        }

        return this;
    }
 }
