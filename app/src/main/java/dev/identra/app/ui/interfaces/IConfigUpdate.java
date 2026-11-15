package dev.identra.app.ui.interfaces;

import dev.identra.app.ui.transactions.ConfigTransactionResult;

public interface IConfigUpdate {
    void onConfigUpdate(ConfigTransactionResult result);
}
