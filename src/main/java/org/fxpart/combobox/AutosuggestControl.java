package org.fxpart.combobox;

import javafx.scene.control.Control;

/**
 * Created by metairie on 07-Jul-15.
 */
public class AutosuggestControl extends Control{

    private String stylesheet;

    protected final String getUserAgentStylesheet(Class<?> clazz, String fileName) {
        if (stylesheet == null) {
            stylesheet = clazz.getResource(fileName).toExternalForm();
        }
        return stylesheet;
    }
}
