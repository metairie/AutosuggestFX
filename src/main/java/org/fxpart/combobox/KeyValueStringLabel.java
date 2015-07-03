package org.fxpart.combobox;

/**
 * Created by laurent on 11.02.2015.
 */
public interface KeyValueStringLabel<T> extends KeyValue<T, String> {

    String getValue();

    T getKey();
}
