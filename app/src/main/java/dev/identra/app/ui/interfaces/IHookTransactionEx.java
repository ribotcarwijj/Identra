package dev.identra.app.ui.interfaces;

import dev.identra.app.ui.transactions.HookTransactionResult;

public interface IHookTransactionEx {
    void onHookUpdate(HookTransactionResult result);
}
