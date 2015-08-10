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
public class AutosuggestComboBoxListSkin<T extends KeyValue> extends BehaviorSkinBase<AutosuggestComboBoxList<T>, BehaviorBase<AutosuggestComboBoxList<T>>> {

    /**************************************************************************
     * fields
     **************************************************************************/
    private static final KeyCodeCombination UP = new KeyCodeCombination(KeyCode.UP);
    private static final KeyCodeCombination DOWN = new KeyCodeCombination(KeyCode.DOWN);
    private static final KeyCodeCombination LEFT = new KeyCodeCombination(KeyCode.LEFT);
    private static final KeyCodeCombination RIGHT = new KeyCodeCombination(KeyCode.RIGHT);
    private static final KeyCodeCombination ENTER = new KeyCodeCombination(KeyCode.ENTER);
    private static final KeyCodeCombination HOME = new KeyCodeCombination(KeyCode.HOME);
    private static final KeyCodeCombination TAB = new KeyCodeCombination(KeyCode.TAB);
    private static final KeyCodeCombination END = new KeyCodeCombination(KeyCode.END);

    // apply style
    private static final String HIGHLIGHTED_DROPDOWN_CLASS = "highlighted-dropdown";
    private static final String USUAL_DROPDOWN_CLASS = "usual-dropdown";

    // visuals
    private final HBox root = new HBox();
    private final HBox visibleBox = new HBox();
    private final HBox hiddenBox = new HBox();
    private final ComboBox<T> combo = new ComboBox<>();
    private final Button button = new Button();
    private DoubleProperty fixedHeight = new SimpleDoubleProperty(150);
    private boolean columnSeparatorVisible = false;

    // data
    private final AutosuggestComboBoxList<T> control;
    private final ObservableList<T> items;
    private String columnSeparator = "|";
    private String keyValueSeparator = " - ";

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

//        // TODO callback
//        control.endControlInitialization();
    }

    private void init() {
        combo.setEditable(control.isEditable());
        combo.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ENTER:
                    if (!getCombo().getEditor().textProperty().get().equalsIgnoreCase("")) {
                        switchNode(combo, button);
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
            control.setSkinStatus(String.valueOf(AutosuggestComboBoxList.STATUS_SKIN.CONTROL_VISIBLE));
        });
        button.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ENTER:
                    switchNode(button, combo);
                    control.setSkinStatus(String.valueOf(AutosuggestComboBoxList.STATUS_SKIN.CONTROL_VISIBLE));
                    e.consume();
            }
        });

        // set factories
        if (control.getStringItemFormatter() != null) {
            setStringCellFactory((Function<T, String>) control.getStringItemFormatter());
        } else {
            setNodeCellFactory((Function<T, Node>) control.getNodeItemFormatter());
        }
        setTextFieldFormatter((Function<T, String>) control.getStringTextFormatter());
        combo.setItems(this.items);
    }

    private void reSchedule(Event event) {
        control.reSchedule(event);
    }

    private void graphical() {
        // building nodes
        root.setPadding(new Insets(1, 1, 1, 1));
        visibleBox.setPadding(new Insets(1, 1, 1, 1));
        combo.getStylesheets().add("org/fxpart/combobox/autosuggestfx.css");
        button.setMaxHeight(Double.MAX_VALUE);
        Image image = new Image(getClass().getResourceAsStream("/org/fxpart/combobox/close.png"));
        button.setContentDisplay(ContentDisplay.RIGHT);
        button.setAlignment(Pos.BASELINE_RIGHT);
        button.setPadding(new Insets(1, 5, 1, 5));
        button.setGraphic(new ImageView(image));
        visibleBox.getChildren().addAll(combo);
        hiddenBox.getChildren().add(button);
        HBox search = new HBox();
        root.getChildren().addAll(visibleBox);
        getChildren().add(root);
    }

    private void bind() {
        button.textProperty().bind(combo.getEditor().textProperty());
    }

    private EventHandler<KeyEvent> createKeyReleaseEventHandler() {
        return event -> {
            if (DOWN.match(event)) {
                if (!combo.isShowing()) {
                    combo.show();
                }
                return;
            } else if (UP.match(event) || RIGHT.match(event) || LEFT.match(event) || HOME.match(event) || END.match(event) || TAB.match(event) || event.isControlDown()) {
                return;
            }

            // search if possible
            if (combo.visibleProperty().getValue()) {
                // TODO REMOVE
                if (combo.getSelectionModel().isEmpty()) {

                }

                reSchedule(event);
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

    private void setStringCellFactory(Function<T, String> itemFormatter) {
        combo.setCellFactory(new Callback<ListView<T>, ListCell<T>>() {
                                 @Override
                                 public ListCell<T> call(ListView<T> param) {
                                     param.setPrefHeight(getFixedHeight());
                                     final ListCell<T> cell = new ListCell<T>() {
                                         @Override
                                         protected void updateItem(T item, boolean empty) {
                                             super.updateItem(item, empty);
                                             if (item == null || empty) {
                                                 setText(null);
                                             } else {
                                                 // render
                                                 setText(itemFormatter.apply(item));
                                             }
                                         }
                                     };
                                     return cell;
                                 }
                             }
        );
    }

    private void setNodeCellFactory(Function<T, Node> itemFormatter) {
        combo.setCellFactory(new Callback<ListView<T>, ListCell<T>>() {
                                 @Override
                                 public ListCell<T> call(ListView<T> param) {
                                     param.setPrefHeight(getFixedHeight());
                                     final ListCell<T> cell = new ListCell<T>() {
                                         @Override
                                         protected void updateItem(T item, boolean empty) {
                                             super.updateItem(item, empty);
                                             if (item == null || empty) {
                                                 setGraphic(null);
                                             } else {
                                                 HBox styledText = new HBox();
                                                 String keyString = ((KeyValueString) item).getKey();
                                                 String valueString = ((KeyValueString) item).getValue();
                                                 String guess = control.getEditorText();

                                                 // key
                                                 if (control.isFullSearch()) {
                                                     styledText = createStyledText(keyString, guess, styledText, control.isIgnoreCase());
                                                 } else {
                                                     styledText.getChildren().add(new Text(keyString));
                                                 }

                                                 // kv separator
                                                 styledText.getChildren().add(new Text(keyValueSeparator));

                                                 // value
                                                 styledText = createStyledText(valueString, guess, styledText, control.isIgnoreCase());

                                                 // render
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
     * create a Text Style Field
     *
     * @param searched
     * @param guess
     * @param styledText
     * @return
     */
    private HBox createStyledText(String searched, String guess, HBox styledText, boolean isIgnoreCase) {
        int index = (isIgnoreCase ? searched.toLowerCase().indexOf(guess.toLowerCase()) : searched.indexOf(guess));
        if (index >= 0) {

            String beginString = searched.substring(0, index);
            String highlightedString = (isIgnoreCase ? searched.substring(index, index + guess.length()) : guess);
            String endString = searched.substring(index + guess.length());

            final Text begin = new Text(beginString);
            styledText.getChildren().add(begin);

            final Text highlighted = new Text(highlightedString);
            highlighted.getStyleClass().add(HIGHLIGHTED_DROPDOWN_CLASS);
            styledText.getChildren().add(highlighted);

            final Text end = new Text(endString);
            end.getStyleClass().add(USUAL_DROPDOWN_CLASS);
            styledText.getChildren().add(end);

        } else {
            styledText.getChildren().add(new Text(searched));
        }
        return styledText;
    }

    /**
     * Method for Swapping belonging to a Node
     *
     * @param item
     * @param newParent
     */
    private void changeParent(Node item, Parent newParent) {
        Parent oldParent = item.getParent();
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

    /**
     * Swap between Button and Combo
     *
     * @param nodeToHide
     * @param nodeToShow
     */
    private void switchNode(Node nodeToHide, Node nodeToShow) {
        changeParent(nodeToHide, hiddenBox);
        changeParent(nodeToShow, visibleBox);
        control.requestFocus();
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

    public String getColumnSeparator() {
        return columnSeparator;
    }

    public void setColumnSeparator(String columnSeparator) {
        this.columnSeparator = columnSeparator;
    }

    public boolean isColumnSeparatorVisible() {
        return columnSeparatorVisible;
    }

    public void setColumnSeparatorVisible(boolean columnSeparatorVisible) {
        this.columnSeparatorVisible = columnSeparatorVisible;
    }

    public String getKeyValueSeparator() {
        return keyValueSeparator;
    }

    public void setKeyValueSeparator(String keyValueSeparator) {
        this.keyValueSeparator = keyValueSeparator;
    }
}
