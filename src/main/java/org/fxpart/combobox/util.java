package org.fxpart.combobox;

import javafx.scene.Node;
import javafx.scene.Parent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Created by metairie on 08.07.2015.
 */
public class util {
    public static void changeParent(Node item, Parent newParent) {
        Parent oldParent = item.getParent();
        // Swapping parent
        try {
            Method oldNode = oldParent.getClass().getMethod("getChildren");
            Method newNode = newParent.getClass().getMethod("getChildren");
            Object ob = oldNode.invoke(oldParent);
            Object nb = newNode.invoke(newParent);
            Collection<Node> cnOld = ((Collection<Node>) ob);
            Collection<Node> cnNew = ((Collection<Node>) nb);
            cnOld.remove(item);
            cnNew.add(item);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}
