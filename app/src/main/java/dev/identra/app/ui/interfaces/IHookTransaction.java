package dev.identra.app.ui.interfaces;

import java.util.List;

import dev.identra.app.api.XResult;
import dev.identra.app.api.hook.assignment.LuaAssignment;

public interface IHookTransaction {
    void onGroupFinished(List<LuaAssignment> assignments, int position, boolean assign, XResult result);
}
