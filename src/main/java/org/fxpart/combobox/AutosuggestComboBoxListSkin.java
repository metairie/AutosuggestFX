package org.fxpart.combobox;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.KeyBinding;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
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

    private static final String HIGHLIGHTED_DROPDOWN_CLASS = "highlighted-dropdown";
    private static final String USUAL_DROPDOWN_CLASS = "usual-dropdown";
    private static final String SEARCH_DROPDOWN_CLASS = "search-dropdown";

    // visuals
    private final HBox root = new HBox();
    private final HBox visibleBox = new HBox();
    private final HBox hiddenBox = new HBox();
    private final ComboBox<T> combo = new ComboBox<>();
    private final Button button = new Button();
    private DoubleProperty fixedHeight = new SimpleDoubleProperty(150);

    // data
    private final AutosuggestComboBoxList<T> control;
    private final ObservableList<T> items;
    private BooleanProperty searchStatus = new SimpleBooleanProperty();

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

        // bindings
        bind();
    }

    private void init() {
        combo.setEditable(control.isEditable());
        combo.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ENTER:
                    if (!getCombo().getEditor().textProperty().get().equalsIgnoreCase("")) {
                        switchNode(combo, button);
                        // TODO binding?
                        control.setSkinStatus(String.valueOf(AutosuggestComboBoxList.STATUS_SKIN.BUTTON_VISIBLE));
                    }
                    e.consume();
            }
        });
        combo.addEventHandler(KeyEvent.KEY_RELEASED, createKeyReleaseEventHandler());
        combo.setOnShown(event -> {
            if (!button.disabledProperty().getValue()) {
                reSchedule(event);
            }
        });
        button.setOnAction(event -> {
            switchNode(button, combo);
            // TODO binding?
            control.setSkinStatus(String.valueOf(AutosuggestComboBoxList.STATUS_SKIN.CONTROL_VISIBLE));
        });

        setCustomCellFactory();
        setTextFieldFormatter((Function<T, String>) control.getTextFieldFormatter());
        combo.setItems(this.items);
    }

    private void reSchedule(Event event) {
        control.reSchedule(event);
    }

    private void graphical() {
        // building nodes
        root.setPadding(new Insets(1, 1, 1, 1));
        visibleBox.setPadding(new Insets(1, 1, 1, 1));
        button.setMaxHeight(Double.MAX_VALUE);
        Image image = new Image(getClass().getResourceAsStream("/org/fxpart/close.png"));
        button.setContentDisplay(ContentDisplay.RIGHT);
        button.setAlignment(Pos.BASELINE_RIGHT);
        button.setPadding(new Insets(1, 5, 1, 5));
        button.setGraphic(new ImageView(image));
        visibleBox.getChildren().addAll(combo);
        hiddenBox.getChildren().add(button);
        root.getChildren().addAll(visibleBox);
        getChildren().add(root);
    }

    private void bind() {
        button.textProperty().bind(combo.getEditor().textProperty());
        searchStatus.bind(control.loadingIndicatorProperty());
        searchStatus.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                combo.getStyleClass().add(SEARCH_DROPDOWN_CLASS);
            } else {
                combo.setStyle("");
            }
        });
    }

    private EventHandler<KeyEvent> createKeyReleaseEventHandler() {
        return new EventHandler<KeyEvent>() {
            private boolean moveCaretToPos = false;
            private int caretPos;

            @Override
            public void handle(KeyEvent event) {
                // nothing to do on this
                if (RIGHT.match(event) || LEFT.match(event) || HOME.match(event) || END.match(event) || TAB.match(event) || event.isControlDown()) {
                    return;
                }
                // action
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

                // do a Scheduled search
                if (!button.disabledProperty().getValue()) {
                    reSchedule(event);
                }
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

    private void setTextFieldFormatter(Function<T, String> textFieldFormatter) {
        combo.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T t) {
                return t == null ? null : textFieldFormatter.apply((T) t);
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
                                     param.setPrefHeight(getFixedHeight());
                                     final ListCell<T> cell = new ListCell<T>() {
                                         @Override
                                         protected void updateItem(T item, boolean empty) {
                                             super.updateItem(item, empty);
                                             if (item == null || empty) {
                                                 //setText(null);
                                                 setGraphic(null);
                                             } else {
                                                 //setText(null);
                                                 HBox styledText = new HBox();
                                                 String keyString = (String) ((KeyValueStringLabel) item).getKey();
                                                 String valueString = ((KeyValueStringLabel) item).getValue();
                                                 String itemString = keyString + " - " + valueString;
                                                 if (control.getEditorText().length() != 0) {
                                                     Integer searchStringPosition = valueString.indexOf(control.getEditorText());

                                                     // itemString contains searchString. It should be split and searchString should be highLighted
                                                     if (searchStringPosition >= 0) {
                                                         String beginString = valueString.substring(0, searchStringPosition);
                                                         String highlightedString = valueString.substring(searchStringPosition, searchStringPosition + control.getEditorText().length());
                                                         String endString = valueString.substring(searchStringPosition + control.getEditorText().length());

                                                         Text separator = new Text(keyString + " - ");
                                                         separator.getStyleClass().add(USUAL_DROPDOWN_CLASS);
                                                         styledText.getChildren().add(separator);

                                                         final Text begin = new Text(beginString);
                                                         begin.getStyleClass().add(USUAL_DROPDOWN_CLASS);
                                                         styledText.getChildren().add(begin);

                                                         final Text highlighted = new Text(highlightedString);
                                                         highlighted.getStyleClass().add(HIGHLIGHTED_DROPDOWN_CLASS);
                                                         styledText.getChildren().add(highlighted);

                                                         final Text end = new Text(endString);
                                                         end.getStyleClass().add(USUAL_DROPDOWN_CLASS);
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

    /**
     * Generic method for Swapping node from an old Parent to a new one
     *
     * @param item
     * @param newParent
     */
    private void changeParent(Node item, Parent newParent) {
        Parent oldParent = item.getParent();
        // Swapping parent
        try {
            Method oldNode = oldParent.getClass().getMethod("getChildren");
            Object ob = oldNode.invoke(oldParent);
            Collection<Node> cnOld = ((Collection<Node>) ob);
            cnOld.remove(item);

            Method newNode = newParent.getClass().getMethod("getChildren");
            Object nb = newNode.invoke(newParent);
            Collection<Node> cnNew = ((Collection<Node>) nb);
            cnNew.add(item);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private void switchNode(Node nodeToHide, Node nodeToShow) {
        changeParent(nodeToHide, hiddenBox);
        changeParent(nodeToShow, visibleBox);
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

    public ComboBox<T> getCombo() {
        return combo;
    }

    public Button getButton() {
        return button;
    }

}
