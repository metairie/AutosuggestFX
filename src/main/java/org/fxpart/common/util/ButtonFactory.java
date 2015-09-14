package org.fxpart.common.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;

/**
 * Created by metairie on 14-Sep-15.
 */
public class ButtonFactory {
    public static Button getNew() {
        Button button = new Button();
        button.setMaxHeight(Double.MAX_VALUE);
        button.setContentDisplay(ContentDisplay.RIGHT);
        button.setAlignment(Pos.BASELINE_RIGHT);
        button.setPadding(new Insets(1, 5, 1, 5));
        return button;
    }
}
