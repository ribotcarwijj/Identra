package dev.identra.app.x.ui.core.dialog;

import dev.identra.app.x.core.database.A_CODE;

public interface IDialogEventFinishObject<T> {
    void onFinish(A_CODE result, T obj);
}
