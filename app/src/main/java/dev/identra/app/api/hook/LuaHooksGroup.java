package dev.identra.app.api.hook;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.CheckBox;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.identra.app.api.app.IdentraApp;
import dev.identra.app.api.hook.assignment.LuaAssignment;
import dev.identra.app.api.settings.LuaSettingExtended;
import dev.identra.app.logger.XLog;
import dev.identra.app.utilities.SettingUtil;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;

public class LuaHooksGroup {
    public CheckBox lastBox = null;

    public String groupId;

    public int id;
    public String name;
    public String title;

    public boolean exception = false;

    public int installed = 0;
    public int optional = 0;
    public long used = -1;
    public int assigned = 0;

    public List<XHook> hooks = new ArrayList<>();

    private String titleName = null;

    public String getCleanTitle() {
        if(titleName == null || titleName.isEmpty()) {
            titleName = SettingUtil.cleanSettingName(title);
        }

        return titleName;
    }

    public boolean hasWarning = false;

    public IdentraApp app;

    public LuaHooksGroup() { }
    public boolean hasException() {
        return (assigned > 0 && exception);
    }
    public boolean hasInstalled() {
        return (assigned > 0 && installed > 0);
    }
    public boolean allInstalled() {
        return (assigned > 0 && installed + optional == assigned);
    }
    public long lastUsed() {
        return used;
    }
    public boolean hasAssigned() {
        return (assigned > 0);
    }

    public boolean allAssigned() {
        return (assigned == hooks.size());
    }
}
