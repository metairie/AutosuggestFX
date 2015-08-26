package org.fxpart.common.bean;

/**
 * Created by metairie on 08.08.2015.
 */
public class KeyValueString implements KeyValue<String, String> {

    private Long id;
    private String value;
    private String key;

    public KeyValueString(Long id, String key, String value) {
        this(value, key);
        this.id = id;
    }

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
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getKey() {
        return key;
    }

}
