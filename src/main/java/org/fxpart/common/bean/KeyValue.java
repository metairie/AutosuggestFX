package org.fxpart.common.bean;

/**
 * Created by laurent on 11.02.2015.
 */
public interface KeyValue<K, V> {

    K getKey();

    V getValue();

    void setKey(K k);

    void setValue(V v);

    void setId(Long id);

    Long getId();
}
