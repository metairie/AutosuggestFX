package org.fxpart.combobox;

import javafx.collections.FXCollections;

import java.util.List;

/**
 * Basic autosuggest
 *
 * @param <T>
 */
public class AutoSuggest2<T> extends AutosuggestFX<T, ValueString<T>> {
    public AutoSuggest2() {
    }

    public AutoSuggest2(List<ValueString<T>> items) {
        super(FXCollections.observableArrayList(items));
    }
}
