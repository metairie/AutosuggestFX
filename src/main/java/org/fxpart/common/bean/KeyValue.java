package org.fxpart.common.bean;

/**
 * Created by metairie on 26.08.2015.
 */
public class KeyValue<K, V> {
    Long id;
    V value;
    K key;

    public V getValue() {
        return value;
    }

    public void setKey(K k) {
        key = k;
    }

    public void setValue(V v) {
        value = v;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public K getKey() {
        return key;
    }

}
