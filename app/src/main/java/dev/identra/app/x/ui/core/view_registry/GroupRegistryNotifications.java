package dev.identra.app.x.ui.core.view_registry;

import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import dev.identra.app.DebugUtil;

public class GroupRegistryNotifications implements ISessionObject {
    private static final String TAG = "Identra.GroupRegistryNotifications";

    public static GroupRegistryNotifications create(SharedRegistry stateRegistry) { return new GroupRegistryNotifications(stateRegistry); }

    private final Map<String, IStateChanged> onChangers = new HashMap<>();
    private final SharedRegistry stateRegistry;

    @Override
    public SharedRegistry getRegistry() { return stateRegistry; }

    @Override
    public String getSessionId() { return stateRegistry.getSessionId(); }

    public GroupRegistryNotifications(SharedRegistry stateRegistry) { this.stateRegistry = stateRegistry; }

    public void notifyGroupChange(String groupToNotify, String groupThatChanged) {
        if(!TextUtils.isEmpty(groupThatChanged) && !TextUtils.isEmpty(groupThatChanged)) {
            if(DebugUtil.isDebug())
                Log.d(TAG, "Notifying State Change, Group to Notify=" + groupToNotify + " Group that Changed=" + groupThatChanged);

            IStateChanged onc = onChangers.get(groupToNotify);
            if(onc != null)
                onc.onGroupChange(ChangedStatesPacket.create(groupThatChanged));
            else
                Log.w(TAG, "Failed to Notify of State Change, Group to Notify does not exist, Notify=" + groupToNotify + " Group that Changed=" + groupThatChanged);
        }
    }

    public void putGroupChangeListener(IStateChanged onChangeListener, String groupName) {
        if(!TextUtils.isEmpty(groupName)) {
            onChangers.put(groupName, onChangeListener);
        }
    }
}
