package org.fxpart.combobox;

import javafx.collections.FXCollections;

import java.util.List;

/**
 * Basic autosuggest
 *
 * @param <T>
 */
public class AutoSuggest1<T> extends AutosuggestFX<T, ValueString<T>> {
    public AutoSuggest1() {
    }

    public AutoSuggest1(List<ValueString<T>> items) {
        super(FXCollections.observableArrayList(items));
    }
}
