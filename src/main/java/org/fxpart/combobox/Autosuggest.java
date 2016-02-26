package org.fxpart.combobox;

import javafx.collections.FXCollections;

import java.util.List;

/**
 * Basic autosuggest
 *
 * @param <T>
 */
public class AutoSuggest<T> extends AutosuggestFX<T, ValueString<T>> {
    public AutoSuggest() {
    }

    public AutoSuggest(List<ValueString<T>> items) {
        super(FXCollections.observableArrayList(items));
    }
}
