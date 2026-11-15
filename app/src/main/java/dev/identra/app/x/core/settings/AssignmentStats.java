package dev.identra.app.x.core.settings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.identra.app.x.Str;
import dev.identra.app.x.data.box.XAssignment;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.ui.core.view_registry.IIdentifiableObject;
import dev.identra.app.x.core.hook.AssignmentPacket;
import dev.identra.app.x.core.hook.IAssignListener;

public class AssignmentStats implements IIdentifiableObject {
    private XHook hook;
    private AssignmentPacket assignment;


    //private final Map<String, IAssignListener> events = new HashMap<>();

    @Override
    public String getObjectId() {
        if(hook != null) return hook.getObjectId();
        if(assignment != null) return assignment.getObjectId();
        return Str.EMPTY;
    }
}
