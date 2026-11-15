package dev.identra.app.ui.interfaces;

import java.util.List;

import dev.identra.app.api.XResult;
import dev.identra.app.api.hook.assignment.LuaAssignment;
import dev.identra.app.api.settings.LuaSettingExtended;

public interface ISettingTransaction {
    void onSettingFinished(LuaSettingExtended setting, int position, XResult result);
}
