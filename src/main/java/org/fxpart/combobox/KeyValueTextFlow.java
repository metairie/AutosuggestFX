package org.fxpart.combobox;

import javafx.scene.text.TextFlow;

/**
 * Created by metairie on 25.06.2015.
 */
public interface KeyValueTextFlow extends KeyValue<String, TextFlow> {

    TextFlow getValue();

    String getKey();

}
