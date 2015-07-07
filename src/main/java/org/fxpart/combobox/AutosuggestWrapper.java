package org.fxpart.combobox;

import javafx.scene.control.TextField;

/**
 * Created by Stephane Metairie on 30.06.2015.
 */
public class AutosuggestWrapper<T> extends AutosuggestComboBoxList<T> {

    TextField textSelected;

    public AutosuggestWrapper() {
        textSelected = new TextField();
    }

}