package dev.identra.app.random.elements;

import android.content.Context;

import dev.identra.app.random.IRandomizerManager;

public interface IManagedSpinnerElement extends ISpinnerElement {
    boolean isNaN();
    void setAsSelected();
    IRandomizerManager getManager();
    String generateString(Context context);
}
