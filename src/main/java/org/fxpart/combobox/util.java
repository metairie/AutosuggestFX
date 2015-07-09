package org.fxpart.combobox;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;

/**
 * Created by metairie on 08.07.2015.
 */
public class util {
    public static void changeParent(Node item, Parent newParent) {
        try {
            // HAve to use reflection, because the getChildren method is protected in common ancestor of all
            // parent nodes.

            // Checking old parent for public getChildren() method
            Parent oldParent = item.getParent();
            if ((oldParent.getClass().getMethod("getChildren").getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
                throw new IllegalArgumentException("Old parent has no public getChildren method.");
            }
            // Checking new parent for public getChildren() method
            if ((newParent.getClass().getMethod("getChildren").getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
                throw new IllegalArgumentException("New parent has no public getChildren method.");
            }

            // Finding common ancestor for the two parents
            Parent commonAncestor = findCommonAncestor(oldParent, newParent);
            if (commonAncestor == null) {
                throw new IllegalArgumentException("Item has no common ancestor with the new parent.");
            }

            // Bounds of the item
            Bounds itemBoundsInParent = item.getBoundsInParent();

            // Mapping coordinates to common ancestor
            Bounds boundsInParentBeforeMove = localToParentRecursive(oldParent, commonAncestor, itemBoundsInParent);

            // Swapping parent
            ((Collection<Node>) oldParent.getClass().getMethod("getChildren").invoke(oldParent)).remove(item);
            ((Collection<Node>) newParent.getClass().getMethod("getChildren").invoke(newParent)).add(item);

            // Mapping coordinates back from common ancestor
            Bounds boundsInParentAfterMove = parentToLocalRecursive(newParent, commonAncestor, boundsInParentBeforeMove);

            // Setting new translation
            item.setTranslateX(
                    item.getTranslateX() + (boundsInParentAfterMove.getMinX() - itemBoundsInParent.getMinX()));
            item.setTranslateY(
                    item.getTranslateY() + (boundsInParentAfterMove.getMinY() - itemBoundsInParent.getMinY()));
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException("Error while switching parent.", e);
        }
    }
}
