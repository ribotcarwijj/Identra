package dev.identra.app.ui.interfaces;

import dev.identra.app.ui.transactions.SettingTransactionResult;

public interface ISettingUpdateEx {
    //void onSettingUpdatedSuccessfully(Context context, LuaSettingExtended setting, XResult result, int position);
    void onSettingUpdate(SettingTransactionResult result);
}
