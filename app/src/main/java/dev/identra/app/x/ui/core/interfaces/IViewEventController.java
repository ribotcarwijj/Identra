package dev.identra.app.x.ui.core.interfaces;

import android.view.View;

import dev.identra.app.x.ui.core.ViewEventController;

public interface IViewEventController {
    void addViewsToEventController(View... views);
    void wire();
    void unWire();
    ViewEventController getViewEventController();
}
