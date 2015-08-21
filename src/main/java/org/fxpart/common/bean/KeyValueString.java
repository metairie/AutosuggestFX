package org.fxpart.common.bean;

/**
 * Created by metairie on 08.08.2015.
 */
public class KeyValueString implements KeyValue<String, String> {

    private String value;
    private String key;

    public KeyValueString(String key, String value) {
        this.value = value;
        this.key = key;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setKey(String k) {
        key = k;
    }

    @Override
    public void setValue(String v) {
        value = v;
    }

    @Override
    public String getKey() {
        return key;
    }

}
