package org.fxpart.combobox;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.KeyBinding;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Collections;

/**
 * Created by metairie on 07-Jul-15.
 */
public class AutosuggestComboBoxListSkin<T> extends BehaviorSkinBase<AutosuggestComboBoxList<T>, BehaviorBase<AutosuggestComboBoxList<T>>> {

    /**************************************************************************
     * fields
     **************************************************************************/
    private static final KeyCodeCombination UP = new KeyCodeCombination(KeyCode.UP);
    private static final KeyCodeCombination DOWN = new KeyCodeCombination(KeyCode.DOWN);
    private static final KeyCodeCombination LEFT = new KeyCodeCombination(KeyCode.LEFT);
    private static final KeyCodeCombination RIGHT = new KeyCodeCombination(KeyCode.RIGHT);
    private static final KeyCodeCombination BACK_SPACE = new KeyCodeCombination(KeyCode.BACK_SPACE);
    private static final KeyCodeCombination DELETE = new KeyCodeCombination(KeyCode.DELETE);
    private static final KeyCodeCombination HOME = new KeyCodeCombination(KeyCode.HOME);
    private static final KeyCodeCombination TAB = new KeyCodeCombination(KeyCode.TAB);
    private static final KeyCodeCombination END = new KeyCodeCombination(KeyCode.END);

    private static final String HIGHLIGHTED_CLASS = "highlighted-dropdown";
    private static final String USUAL_CLASS = "usual-dropdown";

    // visuals
    private final HBox root;
    private final VBox vBoxText;
    private final VBox vBoxCombo;
    private final ComboBox<T> combo;
    private final TextField selectedItem;
    private final ProgressBar progressBar;

    // data
    private final AutosuggestComboBoxList<T> control;
    private final ObservableList<T> items;

    // TODO sort this
    private boolean acceptFreeValue = false;
    private DoubleProperty fixedHeight = new SimpleDoubleProperty(150);
    private int visibleRowsCount = 10;
    private boolean lazyMode = true;
    private boolean loadingIndicator = false;
    private int timer = 1000;
    private boolean editable = true;

    /**************************************************************************
     * Constructors
     **************************************************************************/
    public AutosuggestComboBoxListSkin(final AutosuggestComboBoxList<T> control) {
        super(control, new BehaviorBase<>(control, Collections.<KeyBinding>emptyList()));
        this.control = control;
        items = control.getItems();
        root = new HBox();
        vBoxCombo = new VBox();
        vBoxText = new VBox();
        progressBar = new ProgressBar();
        selectedItem = new TextField();

        // build control up
        combo = new ComboBox();
        combo.setEditable(true);
        combo.addEventHandler(KeyEvent.KEY_RELEASED, createKeyReleaseEventHandler());

        init();

        // visual aspect
        root.setStyle("-fx-background-color: #336699;");
        root.setPadding(new Insets(1, 1, 1, 1));

        vBoxCombo.setStyle("-fx-background-color: #FFFFBB;");
        vBoxCombo.setPadding(new Insets(1, 1, 1, 1));
        progressBar.setMaxWidth(Double.MAX_VALUE);
        vBoxCombo.getChildren().add(progressBar);
        vBoxCombo.getChildren().add(combo);

        vBoxText.setStyle("-fx-background-color: #AAFFBB;");
        vBoxText.setPadding(new Insets(6, 1, 0, 1));
        selectedItem.setVisible(true);
        selectedItem.setMaxHeight(Double.MAX_VALUE);
        vBoxText.getChildren().add(selectedItem);

        root.getChildren().add(vBoxText);
        root.getChildren().add(vBoxCombo);
        getChildren().add(root);
    }

    private void init() {
        control.setCombo(combo);
        combo.setItems(control.getItems());
    }

    public void doSearch(Event event) {
        DelayedSearchTask delayedSearchTask = new DelayedSearchTask(this.control, getTimer(), event);
        Thread delayedSearchThread = new Thread(delayedSearchTask);
        delayedSearchThread.start();
    }

    protected EventHandler<KeyEvent> createKeyReleaseEventHandler() {
        return new EventHandler<KeyEvent>() {
            private boolean moveCaretToPos = false;
            private int caretPos;

            @Override
            public void handle(KeyEvent event) {
                String term = combo.getEditor().getText();
                int termLength = 0;

                if (term != null) {
                    termLength = term.length();
                }

                if (UP.match(event)) {
                    caretPos = -1;
                    moveCaret(termLength);
                    return;
                } else if (DOWN.match(event)) {
                    if (!combo.isShowing()) {
                        combo.show();
                    }
                    caretPos = -1;
                    moveCaret(termLength);
                    return;
                } else if (BACK_SPACE.match(event)) {
                    moveCaretToPos = true;
                    caretPos = combo.getEditor().getCaretPosition();
                } else if (DELETE.match(event)) {
                    moveCaretToPos = true;
                    caretPos = combo.getEditor().getCaretPosition();
                }

                if (RIGHT.match(event) || LEFT.match(event) || event.isControlDown() || HOME.match(event) || END.match(event) || TAB.match(event)) {
                    return;
                }

                if (combo.getValue() != null) {
                    control.setWaitFlag(true);
                }

                // do search
                doSearch(event);

                if (!moveCaretToPos) {
                    caretPos = -1;
                }
                moveCaret(termLength);
            }

            private void moveCaret(int textLength) {
                if (caretPos == -1) {
                    combo.getEditor().positionCaret(textLength);
                } else {
                    combo.getEditor().positionCaret(caretPos);
                }
                moveCaretToPos = false;
            }
        };
    }

    public Integer getTimer() {
        return timer;
    }

    public void setTimer(Integer timer) {
        this.timer = timer;
    }

    public void setLazyMode(boolean lazyMode) {
        this.lazyMode = lazyMode;
    }

    public boolean getLazyMode() {
        return lazyMode;
    }

    public boolean setLoadingIndicator() {
        return loadingIndicator;
    }

    public void setLoadingIndicator(boolean loadingIndicator) {
        this.loadingIndicator = loadingIndicator;
    }

    public boolean isLazyMode() {
        return lazyMode;
    }

    public boolean isLoadingIndicator() {
        return loadingIndicator;
    }

    public int getVisibleRowsCount() {
        return visibleRowsCount;
    }

    public void setVisibleRowsCount(int visibleRowsCount) {
        this.visibleRowsCount = visibleRowsCount;
    }

    public double getFixedHeight() {
        return fixedHeight.get();
    }

    public DoubleProperty fixedHeightProperty() {
        return fixedHeight;
    }

    public void setFixedHeight(double fixedHeight) {
        this.fixedHeight.set(fixedHeight);
    }

    public boolean isAcceptFreeValue() {
        return acceptFreeValue;
    }

    public void setAcceptFreeValue(boolean acceptFreeValue) {
        this.acceptFreeValue = acceptFreeValue;
    }

}
