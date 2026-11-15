package dev.identra.app.api.hook.assignment;

//import dev.identra.app.XAssignment;

import dev.identra.app.api.hook.IdentraHook;

public class IdentraAssignmentBase {
    protected IdentraHook hook;
    protected long installed = -1;
    protected long used = -1;
    protected boolean restricted = false;
    protected String exception;

    public IdentraAssignmentBase() { }
    public IdentraAssignmentBase(IdentraHook hook) {
        this.hook = hook;
    }

    public IdentraHook getHook() { return this.hook; }
    public long getInstalled() { return this.installed; }
    public long getUsed() { return this.used; }
    public boolean getRestricted() { return this.restricted; }
    public String getException() { return this.exception; }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IdentraAssignmentBase))
            return false;
        IdentraAssignmentBase other = (IdentraAssignmentBase) obj;
        return this.hook.getObjectId().equals(other.hook.getObjectId());
    }

    @Override
    public int hashCode() {
        return this.hook.getObjectId().hashCode();
    }
}
