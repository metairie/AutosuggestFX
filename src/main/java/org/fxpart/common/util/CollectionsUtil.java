package org.fxpart.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by metairie on 19.08.2015.
 */
public class CollectionsUtil {
    public static <T> Collection<? extends T> split(Collection<? extends T> d, int n) {
        Collection<T> tmp = new ArrayList<>();
        Iterator it = d.iterator();
        int k = Math.max(0, n);
        while (it.hasNext() && k > 0) {
            tmp.add((T) it.next());
            k--;
        }
        return tmp;
    }

    public static <T> void print(Collection<T> c) {
        Iterator<T> it = c.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }
}
