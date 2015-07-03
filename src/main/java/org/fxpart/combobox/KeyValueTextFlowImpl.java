package org.fxpart.combobox;

import javafx.scene.text.TextFlow;

/**
 * Created by metairie on 29-Jun-15.
 */
public class KeyValueTextFlowImpl implements KeyValueTextFlow {

    private String key;
    private TextFlow value;

    public KeyValueTextFlowImpl(String key, TextFlow value) {
        this.value = value;
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public TextFlow getValue() {
        return value;
    }

}
