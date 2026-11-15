package dev.identra.app.api.xstandard.interfaces;

import android.content.Context;

import java.util.List;

import dev.identra.app.api.XResult;
import dev.identra.app.api.settings.LuaSettingExtended;

public interface ISettingUpdate {
    void onSettingUpdatedSuccessfully(Context context, final LuaSettingExtended setting, XResult result);
    void onSettingUpdateFailed(Context context, final LuaSettingExtended setting, XResult result);
    void onBatchFinished(Context context, List<LuaSettingExtended> successful, List<LuaSettingExtended> failed);
    void onException(Context context, Exception e, LuaSettingExtended setting);
}
