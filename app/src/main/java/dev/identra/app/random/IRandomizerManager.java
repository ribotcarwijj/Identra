package dev.identra.app.random;

import android.content.Context;

import dev.identra.app.random.elements.IManagedSpinnerElement;

public interface IRandomizerManager {
    boolean hasNaNSelected();
    void setSelectedElement(IManagedSpinnerElement el);
    IManagedSpinnerElement getSelectedElement();
    String generateString(Context context);
}
