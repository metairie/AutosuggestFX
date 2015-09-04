package org.fxpart.common;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.WeakListener;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class helps to weak register/deregister
 * <p>
 * Created by metairie on 04-Sep-15.
 */
public class WeakBinder {
    private final List<Object> hardRefs = new ArrayList<>();
    private final Map<ObservableValue<?>, WeakListener> listeners = new HashMap<>();

    public void unbindAllInvalidationListener() {
        for (ObservableValue<?> o : listeners.keySet()) {
            o.removeListener((InvalidationListener) listeners.get(o));
        }
        clear();
    }

    public <T> void bindInvalidationListener(final Property<T> property, final ObservableValue<? extends T> dest) {
        InvalidationListener il = observable -> property.setValue(dest.getValue());
        WeakInvalidationListener wil = new WeakInvalidationListener(il);
        listeners.put(dest, wil);
        dest.addListener(wil);
        property.setValue(dest.getValue());
        hardRefs.add(dest);
        hardRefs.add(il);
    }

    private void clear() {
        hardRefs.clear();
        listeners.clear();
    }

//
//    public <T> void bindChangeListener(final Property<T> property, final ObservableValue<? extends T> dest) {
//        ChangeListener cl = (o, old, n) -> property.setValue(dest.getValue());
//        WeakChangeListener wcl = new WeakChangeListener(cl);
//        listeners.put(dest, wcl);
//        dest.addListener(wcl);
//        property.setValue(dest.getValue());
//        hardRefs.add(dest);
//        hardRefs.add(cl);
//    }

}