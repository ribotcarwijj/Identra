package dev.identra.app.x.hook.filter;

import java.util.List;
import java.util.Map;

import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;

public interface IFilterContainer {
    String getGroupName();
    boolean hasSwallowedAsRule(XHook hook);
    boolean hasSwallowedAsDefinition(XHook hook);
    void initializeDefinitions(List<XHook> hooks, Map<String, String> settings);


    List<String> getDependencies();
    List<XHook> getRules();
    List<XHook> getFilterBases();

    boolean hasSettings();

    int appendSettings(Map<String, String> settings);
}
