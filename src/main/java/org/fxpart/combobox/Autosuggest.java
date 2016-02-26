package org.fxpart.combobox;

import javafx.collections.FXCollections;

import java.util.List;

/**
 * Basic autosuggest
 *
 * @param <T>
 */
public class Autosuggest<T> extends AutosuggestFX<T, ValueString<T>> {
    public AutoSuggest() {
    }

    public Autosuggest(List<ValueString<T>> items) {
        super(FXCollections.observableArrayList(items));
    }
}
