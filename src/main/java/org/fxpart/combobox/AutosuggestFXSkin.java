package org.fxpart.combobox;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.KeyBinding;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
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
import org.fxpart.common.bean.KeyValue;
import org.fxpart.common.bean.KeyValueString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

/**
 * Created by metairie on 07-Jul-15.
 */
public class AutosuggestFXSkin<B, T extends KeyValue> extends BehaviorSkinBase<AutosuggestFX<B, T>, BehaviorBase<AutosuggestFX<B, T>>> {
    private final static Logger LOG = LoggerFactory.getLogger(AutosuggestFXSkin.class);

    /**************************************************************************
     * fields
     **************************************************************************/
    private static final KeyCodeCombination ENTER = new KeyCodeCombination(KeyCode.ENTER);
    private static final KeyCodeCombination ESCAPE = new KeyCodeCombination(KeyCode.ESCAPE);
    private static final KeyCodeCombination UP = new KeyCodeCombination(KeyCode.UP);
    private static final KeyCodeCombination DOWN = new KeyCodeCombination(KeyCode.DOWN);
    private static final KeyCodeCombination LEFT = new KeyCodeCombination(KeyCode.LEFT);
    private static final KeyCodeCombination RIGHT = new KeyCodeCombination(KeyCode.RIGHT);
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
    private final HBox imageBox = new HBox();
    private final ComboBox<T> combo = new ComboBox<>();
    private final Button button = new Button();
    private DoubleProperty fixedHeight = new SimpleDoubleProperty(150);
    private boolean columnSeparatorVisible = false;
    private ImageView ivWait = new ImageView(new Image(getClass().getResourceAsStream("/org/fxpart/combobox/wait16.gif")));
    private ImageView ivClose = new ImageView(new Image(getClass().getResourceAsStream("/org/fxpart/combobox/close16.png")));

    // local data
    private final AutosuggestFX<B, T> control;
    private ObservableList<T> items = null;
    private String columnSeparator = "|";
    private String keyValueSeparator = " - ";
    private String userInput = ""; // sometimes txt editor is reset, must be saved here
    private boolean isSelectedItem = false;
    private ChangeListener itemListener = null;

    /**************************************************************************
     * Constructors
     **************************************************************************/

    /**
     * Default constructor
     * </p>
     * It's an empty control no Item (or value) are set at this level
     * Possible usage :
     * - in a Search Form
     * - when displaying a new Entity Form
     *
     * @param control
     */
    public AutosuggestFXSkin(final AutosuggestFX<B, T> control) {
        super(control, new BehaviorBase<>(control, Collections.<KeyBinding>emptyList()));
        this.control = control;
        initSkin();

        // visual aspect
        graphical();

        if (control.isRefreshFXML()) {
            // TODO #0 null possible
            // When FXML initialize component , never pass here
            isSelectedItem = (((ObjectProperty<T>) control.itemProperty()).getValue() != null);
            userInput = control.itemProperty().getValue().getValue().toString();
            combo.valueProperty().setValue(control.itemProperty().getValue());
            showButton();
        }
    }

    /**
     * Constructor with an T Item passed
     * </p>
     * this.control.item is already feeded by developer
     * Possible usage :
     * - when you want to display an Entity Form with data retrieved from a service
     *
     * @param control
     * @param item
     */
    public AutosuggestFXSkin(final AutosuggestFX<B, T> control, ObjectProperty<T> item) {
        super(control, new BehaviorBase<>(control, Collections.<KeyBinding>emptyList()));
        this.control = control;
        initSkin();

        // visual aspect
        graphical();

        // TODO #0 null possible
        // When FXML initialize component , never pass here
        isSelectedItem = (((ObjectProperty<T>) item).getValue() != null);
        userInput = item.getValue().getValue().toString();
        combo.valueProperty().setValue(item.getValue());
        showButton();
    }

    /**
     * refresh is only called from control
     *
     * @param item
     */
    private void refresh(Observable item) {
        ObjectProperty ob = (ObjectProperty) item;
        if (item != null) {
            T t = (T) ob.getValue();
            if (t != null && t.getValue() != null) {
                userInput = String.valueOf(t.getValue());
                button.textProperty().setValue(String.valueOf(t.getValue()));
            } else {
                button.textProperty().setValue(userInput);
            }
            combo.valueProperty().setValue(t);
        } else {
            userInput = "";
            combo.getEditor().setText("");
            button.textProperty().setValue("");
            combo.valueProperty().setValue(null);
            if (!control.isControlShown()) {
                showCombo();
            }
        }
    }

    private void bind() {
        // item listener
        itemListener = (t, o, n) -> {
            isSelectedItem = (((ObjectProperty<T>) t).getValue() != null);
            // TODO rework
            control.refresh(t);
            refresh(t);
        };
        control.itemProperty().addListener(itemListener);

        // when loading indicator is false. caret is put a the end of the text
        control.filteringIndicatorProperty().addListener(observable -> {
            if (!((BooleanProperty) observable).getValue()) {
                combo.getEditor().positionCaret(combo.getEditor().getText().length());
            }
        });

        // icone wait displayed on control load indicator value
        ivWait.visibleProperty().bind(control.filteringIndicatorProperty());
    }

    /**
     * validate the value
     */
    private void validateInput() {
        userInput = combo.getEditor().getText();

        if (userInput.equalsIgnoreCase("")) {
            combo.getEditor().setText("");
            button.textProperty().setValue("");
            combo.valueProperty().setValue(null);
            control.itemProperty().setValue(null);
            return;
        }
        if (combo.valueProperty().getValue() == null && !control.isAcceptFreeTextValue()) {
            combo.getEditor().setText("");
            button.textProperty().setValue("");
            combo.valueProperty().setValue(null);
            control.itemProperty().setValue(null);
            return;
        }
        if (control.isAcceptFreeTextValue() && (combo.getSelectionModel().getSelectedItem() == null || combo.valueProperty().getValue() == null)) {
            control.itemProperty().setValue(control.newInstanceOfT.apply(null));
            showButton();
            return;
        }
        if (combo.valueProperty().getValue() != null) {
            control.itemProperty().setValue(combo.valueProperty().getValue());
            showButton();
            return;
        }
    }

    private void initSkin() {
        this.items = control.getItems();

        // build control up
        combo.setEditable(control.isEditable());
        combo.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            e.consume();
            if (DOWN.match(e)) {
                if (!combo.isShowing()) {
                    combo.show();
                }
                return;
            } else if (ESCAPE.match(e) || UP.match(e) || RIGHT.match(e) || LEFT.match(e) || HOME.match(e) || END.match(e) || TAB.match(e) || e.isControlDown()) {
                return;
            } else if (ENTER.match(e)) {
                validateInput();
            } else {
                combo.valueProperty().setValue(null);
            }

            // search if possible
            if (combo.visibleProperty().getValue()) {
                reSchedule(e);
                if (!combo.isShowing()) {
                    combo.show();
                }
            }
        });
        combo.setOnShown(event -> {
            reSchedule(event);
        });

        button.setOnAction(event -> {
            event.consume();
            showCombo();
            combo.getEditor().setText(userInput);
        });
        button.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            switch (e.getCode()) {
                case ENTER:
                    e.consume();
                    showCombo();
                    combo.getEditor().setText(userInput);
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

        // bindings
        bind();

        // lost focus
        combo.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // lost focus
            if (oldValue && !newValue) {
                if (control.isControlShown()) {
                    validateInput();
                }
            }
        });
    }

    /**************************************************************************
     * Public API
     **************************************************************************/
    public void showCombo() {
        control.setControlShown(new Boolean(true));
        Platform.runLater(() -> {
            exchangeNode(button, combo);
            combo.requestFocus();
        });
    }

    public void showButton() {
        control.setControlShown(new Boolean(false));
        Platform.runLater(() -> {
            exchangeNode(combo, button);
            button.requestFocus();
        });
        button.textProperty().setValue(userInput);
    }

    private void reSchedule(Event event) {
        control.reSchedule(event, control.isAlwaysRefresh());
    }

    private void graphical() {
        // building nodes
        root.setPadding(new Insets(1, 1, 1, 1));
        visibleBox.setPadding(new Insets(1, 1, 1, 1));
        combo.getStylesheets().add("org/fxpart/combobox/autosuggestfx.css");
        button.setMaxHeight(Double.MAX_VALUE);
        button.setContentDisplay(ContentDisplay.RIGHT);
        button.setAlignment(Pos.BASELINE_RIGHT);
        button.setPadding(new Insets(1, 5, 1, 5));
        button.setGraphic(ivClose);
        if (isSelectedItem) {
            visibleBox.getChildren().add(button);
            hiddenBox.getChildren().add(combo);
        } else {
            visibleBox.getChildren().add(combo);
            hiddenBox.getChildren().add(button);
        }
        imageBox.getChildren().add(ivWait);
        imageBox.setPadding(new Insets(5, 3, 1, 3));
        root.getChildren().addAll(visibleBox, imageBox);
        getChildren().add(root);
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
    private void exchangeNode(Node nodeToHide, Node nodeToShow) {
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

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public boolean isSelectedItem() {
        return isSelectedItem;
    }

    public void setIsSelectedItem(boolean isSelectedItem) {
        this.isSelectedItem = isSelectedItem;
    }

    public void debug(String title) {
        LOG.debug(" --- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");
        LOG.debug(" --- >>>>       " + title);
        LOG.debug(" --- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");
        LOG.debug(" --- combo Editor txt               : " + combo.getEditor().textProperty().getValue());
        LOG.debug(" --- combo T VALUE                  : " + combo.valueProperty().getValue());
        LOG.debug(" --- combo T VALUE getValue         : " + (combo.valueProperty().getValue() == null ? "" : combo.valueProperty().getValue().getValue()));
        LOG.debug(" --- combo Index selected           : " + combo.getSelectionModel().getSelectedIndex());
        LOG.debug(" ------------------------------------");
        LOG.debug(" --- button text                    : " + button.textProperty().getValue());
        LOG.debug(" ------------------------------------");
        LOG.debug(" --- control T ITEM                 : " + control.itemProperty().getValue());
        LOG.debug(" --- control T ITEM getValue        : " + (control.itemProperty().getValue() == null ? "" : control.itemProperty().getValue().getValue()));
        LOG.debug(" --- control T ITEMS LIST           : " + control.getItems().size());
        LOG.debug(" --- control  isSelected            : " + isSelectedItem);
        LOG.debug(" --- control T BEAN                 : " + control.beanProperty().getValue());
        LOG.debug(" --- control T BEAN NOT POSSIBLE    : " + (control.beanProperty().getValue() == null ? "" : control.beanProperty().getValue()));
        LOG.debug(" --- control  isControlShown        : " + control.isControlShown());
        LOG.debug(" --- control  isAcceptFreeTextValue : " + control.isAcceptFreeTextValue());
        LOG.debug(" --- control  isFullSearch          : " + control.isFullSearch());
        LOG.debug(" --- control  isIgnoreCase          : " + control.isIgnoreCase());
        LOG.debug(" --- control  isLazyMode            : " + control.isLazyMode());
        LOG.debug(" --- <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< ");
    }
}
