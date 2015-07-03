package org.fxpart.combobox;

/**
 * Created by laurent on 11.02.2015.
 */
public class KeyValueStringImpl implements KeyValueString {
    private String value;
    private String key;

    public KeyValueStringImpl(String key, String value) {
        this.value = value;
        this.key = key;
    }

    @Override
    public String getValue() {
        return value;
    }


    @Override
    public String getKey() {
        return key;
    }


}