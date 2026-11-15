package dev.identra.app.ui.dialogs;

import dev.identra.app.api.settings.LuaSettingExtended;
import dev.identra.app.api.settings.LuaSettingPacket;
import dev.identra.app.ui.interfaces.ISettingTransaction;

public interface ISettingDialogListener {
    void pushSettingPacket(LuaSettingPacket packet);
    void pushSettingPacket(LuaSettingPacket packet, LuaSettingExtended original, int position, ISettingTransaction transactionCallback);
}
