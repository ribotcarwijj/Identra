package dev.identra.app.ui.transactions;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.api.hook.assignment.LuaAssignment;
import dev.identra.app.api.hook.assignment.LuaAssignmentPacket;
import dev.identra.app.api.properties.MockPropPacket;
import dev.identra.app.api.properties.MockPropSetting;
import dev.identra.app.ui.HookGroup;
import dev.identra.app.utilities.CollectionUtil;

public class HookTransactionResult extends Transaction {
    public List<IdentraHook> hooks = new ArrayList<>();
    public List<LuaAssignmentPacket> packets = new ArrayList<>();
    public List<IdentraHook> failed = new ArrayList<>();
    public List<IdentraHook> succeeded = new ArrayList<>();
    public List<LuaAssignment> assignments = new ArrayList<>();

    public HookGroup group;

    public int getAdapterPosition() { return hooks.size() == 1 ? adapterPosition : -1; }
    public boolean hasAnyFailed() { return CollectionUtil.isValid(failed); }
    public boolean hasAnySucceeded() { return CollectionUtil.isValid(succeeded); }
    public boolean isBatch() { return hooks != null && hooks.size() > 1; }

    public LuaAssignmentPacket getPacket() { return !packets.isEmpty() ? packets.get(0) : null; }
    public LuaAssignmentPacket getPacket(int index) { return !packets.isEmpty() && packets.size() > index ? packets.get(index) : null;  }
    public IdentraHook getSucceeded() { return hasAnySucceeded() ? succeeded.get(0) : null; }
    public IdentraHook getSucceeded(int index) { return hooks.size() > index ? succeeded.get(index) : null; }
    public IdentraHook getFailed() { return hasAnyFailed() ? failed.get(0) : null; }
    public IdentraHook getFailed(int index) { return failed.size() > index ? failed.get(index) : null; }
    public IdentraHook getHook() { return !hooks.isEmpty() ? hooks.get(0) : null; }
    public IdentraHook getHook(int index) { return  hooks.size() > index ? hooks.get(index) : null; }

    public LuaAssignment getAssignment() { return !assignments.isEmpty() ? assignments.get(0) : null; }
    public LuaAssignment getAssignment(int index) { return  assignments.size() > index ? assignments.get(index) : null; }

    @NonNull
    @Override
    public String toString() {
        return new StringBuilder()
                .append(super.toString())
                .append(" succeeded=")
                .append(succeeded.size()).append("\n")
                .append(" failed=")
                .append(failed.size()).append("\n")
                .append(" hooks=")
                .append(hooks.size()).toString();
    }
}
