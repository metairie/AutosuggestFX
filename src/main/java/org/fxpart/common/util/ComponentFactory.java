package org.fxpart.common.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;

/**
 * Created by metairie on 14-Sep-15.
 */
public class ComponentFactory {
    public static Button getNewButton() {
        Button o = new Button();
        o.setMaxHeight(Double.MAX_VALUE);
        o.setContentDisplay(ContentDisplay.RIGHT);
        o.setAlignment(Pos.BASELINE_RIGHT);
        o.setPadding(new Insets(1, 5, 1, 5));
        return o;
    }

    public static TextField getNewTextField() {
        TextField o = new TextField();
        o.setMaxHeight(Double.MAX_VALUE);
        o.setAlignment(Pos.BASELINE_RIGHT);
        o.setPadding(new Insets(1, 5, 1, 5));
        return o;
    }
}
