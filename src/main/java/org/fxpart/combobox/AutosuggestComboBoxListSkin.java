package org.fxpart.combobox;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.KeyBinding;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.util.Collections;
import java.util.function.Function;

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
    private final HBox root = new HBox();
    private final VBox vBoxText = new VBox();
    private final VBox vBoxCombo = new VBox();
    private final ComboBox<T> combo = new ComboBox<>();
    private final TextField selectedItem = new TextField();
    private final ProgressBar progressBar = new ProgressBar();

    // data
    private final AutosuggestComboBoxList<T> control;
    private final ObservableList<T> items;

    // TODO qualify this properties (data?visual?control?)
    private boolean acceptFreeValue = false;
    private DoubleProperty fixedHeight = new SimpleDoubleProperty(150);
    private int visibleRowsCount = 10;
    private boolean lazyMode = true;
    private boolean loadingIndicator = false;
    private boolean editable = true;
    private int timer = 500;

    /**************************************************************************
     * Constructors
     **************************************************************************/
    public AutosuggestComboBoxListSkin(final AutosuggestComboBoxList<T> control) {
        super(control, new BehaviorBase<>(control, Collections.<KeyBinding>emptyList()));
        this.control = control;
        items = control.getItems();

        // build control up
        init();

        // visual aspect
        graphical();
    }

    private void graphical() {
        root.setStyle("-fx-background-color: #336699;");
        root.setPadding(new Insets(1, 1, 1, 1));

        vBoxCombo.setStyle("-fx-background-color: #FFFFBB;");
        vBoxCombo.setPadding(new Insets(1, 1, 1, 1));
        progressBar.setMaxWidth(Double.MAX_VALUE);
        vBoxCombo.getChildren().add(progressBar);
        vBoxCombo.getChildren().add(combo);

        vBoxText.setStyle("-fx-background-color: #AAFFBB;");
        vBoxText.setPadding(new Insets(6, 1, 0, 1));
        selectedItem.setVisible(false);
        selectedItem.setMaxHeight(Double.MAX_VALUE);
        vBoxText.getChildren().add(selectedItem);

        root.getChildren().add(vBoxText);
        root.getChildren().add(vBoxCombo);
        getChildren().add(root);

    }

    private void init() {
        combo.setEditable(editable);
        combo.addEventHandler(KeyEvent.KEY_RELEASED, createKeyReleaseEventHandler());
        control.setCombo(combo);
        setCustomCellFactory();
        setTextFieldFormatter(control.getTextFieldFormatter());
        control.setTimer(timer);
        combo.setItems(control.getItems());
    }

    private EventHandler<KeyEvent> createKeyReleaseEventHandler() {
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
                control.doSearch(event);

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

    private void setTextFieldFormatter(Function<KeyValueString, String> textFieldFormatter) {
        combo.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T t) {
                return t == null ? null : textFieldFormatter.apply((KeyValueString) t);
            }

            @Override
            public T fromString(String string) {
                return combo.getValue();
            }
        });
    }

    private void setCustomCellFactory() {
        combo.setCellFactory(new Callback<ListView<T>, ListCell<T>>() {
                                 @Override
                                 public ListCell<T> call(ListView<T> param) {
                                     //TODO param.setPrefHeight(getFixedHeight());
                                     final ListCell<T> cell = new ListCell<T>() {
                                         @Override
                                         protected void updateItem(T item, boolean empty) {
                                             super.updateItem(item, empty);
                                             if (item == null || empty) {
                                                 setText(null);
                                                 setGraphic(null);
                                             } else {
                                                 setText(null);
                                                 HBox styledText = new HBox();
                                                 String keyString = (String) ((KeyValueStringLabel) item).getKey();
                                                 String valueString = ((KeyValueStringLabel) item).getValue();
                                                 String itemString = keyString + " - " + valueString;
                                                 if (control.getSearchString().length() != 0) {
                                                     Integer searchStringPosition = valueString.indexOf(control.getSearchString());

                                                     // itemString contains searchString. It should be split and searchString should be highLighted
                                                     if (searchStringPosition >= 0) {
                                                         String beginString = valueString.substring(0, searchStringPosition);
                                                         String highlightedString = valueString.substring(searchStringPosition, searchStringPosition + control.getSearchString().length());
                                                         String endString = valueString.substring(searchStringPosition + control.getSearchString().length());

                                                         Text separator = new Text(keyString + " - ");
                                                         separator.getStyleClass().add(USUAL_CLASS);
                                                         styledText.getChildren().add(separator);

                                                         final Text begin = new Text(beginString);
                                                         begin.getStyleClass().add(USUAL_CLASS);
                                                         styledText.getChildren().add(begin);

                                                         final Text highlighted = new Text(highlightedString);
                                                         highlighted.getStyleClass().add(HIGHLIGHTED_CLASS);
                                                         styledText.getChildren().add(highlighted);

                                                         final Text end = new Text(endString);
                                                         end.getStyleClass().add(USUAL_CLASS);
                                                         styledText.getChildren().add(end);


                                                     } else {
                                                         styledText.getChildren().add(new Text(itemString));
                                                     }
                                                 } else {
                                                     styledText.getChildren().add(new Text(itemString));
                                                 }
                                                 setGraphic(styledText);
                                             }
                                         }
                                     };
                                     return cell;
                                 }
                             }
        );
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
