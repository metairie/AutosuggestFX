package org.fxpart.common.bean;

/**
 * Created by metairie on 08.08.2015.
 */
public class KeyValueString extends KeyValue<String, String> {

    public KeyValueString() {
    }

    public KeyValueString(Long id, String key, String value) {
        this(key, value);
        this.id = id;
    }

    public KeyValueString(String key, String value) {
        this.value = value;
        this.key = key;
    }
}
