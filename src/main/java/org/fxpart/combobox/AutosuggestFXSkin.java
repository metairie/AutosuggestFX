package org.fxpart.combobox;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.KeyBinding;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.fxpart.common.WeakBinder;
import org.fxpart.common.bean.KeyValue;
import org.fxpart.common.util.ComponentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Created by metairie on 07-Jul-15.
 */
public class AutosuggestFXSkin<B, T extends KeyValue> extends BehaviorSkinBase<AutosuggestFX<B, T>, BehaviorBase<AutosuggestFX<B, T>>> {
    private final static Logger LOG = LoggerFactory.getLogger(AutosuggestFXSkin.class);
    private static final String DEFAULT_STYLE_CLASS = "autosuggestfx";

    /**************************************************************************
     * fields
     **************************************************************************/
    private static final KeyCodeCombination ENTER = new KeyCodeCombination(KeyCode.ENTER);
    private static final KeyCodeCombination BACKSPACE = new KeyCodeCombination(KeyCode.BACK_SPACE);
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
    //    private final HBox visibleBox = new HBox();
//    private final HBox hiddenBox = new HBox();
//    private final HBox imageBox = new HBox();
    private final ComboBox<T> combo = new ComboBox<>();
    private final List<Button> button = new ArrayList<>();
    // local data
    private final AutosuggestFX<B, T> control;
    // listeners & binds, events
    private final WeakBinder binder = new WeakBinder();
    private Button currentButton = null;
    private int index = 0;
    private DoubleProperty fixedHeight = new SimpleDoubleProperty(150);
    private boolean columnSeparatorVisible = false;
    private ImageView iconWait = new ImageView(new Image(getClass().getResourceAsStream("/org/fxpart/combobox/wait16.gif")));
    private ImageView iconClose = new ImageView(new Image(getClass().getResourceAsStream("/org/fxpart/combobox/close16.png")));
    private ObservableList<T> items = null;
    private String columnSeparator = "|";
    private String keyValueSeparator = " - ";
    private String userInput = ""; // sometimes txt editor is reset, must be saved here
    private boolean isSelectedItem = false;
    private ChangeListener<KeyValue> itemListener = null;
    private InvalidationListener loadingIndicatorListener = null;
    private ChangeListener<Boolean> focusListener = null;
    private ChangeListener<Boolean> getFocusListener = null;
    private EventHandler filterComboKeyReleased = null;
    private EventHandler filterButtonKeyReleased = null;

    /**************************************************************************
     * Constructors
     **************************************************************************/
    /**
     * Gather all bindings
     */
    private boolean quit = false;

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
        this.control.getStyleClass().add(DEFAULT_STYLE_CLASS);

        // visual aspect
        graphical();

        initSkin();

        // init AutosuggestFXSkin without value
        Platform.runLater(() -> {
            combo.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            combo.setPrefWidth(root.getWidth());
        });

        if (control.isRefreshFXML()) {
            refreshSkinWithItem(control.itemProperty());
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

        // visual aspect
        graphical();

        initSkin();

        // at startup only
        refreshSkinWithItem(item);
    }

    /**
     * This method is called by the constructor
     */
    private void initSkin() {
        this.items = control.getItems();

        // events
        buildEvents();

        // formatter factories
        getSkinnable().graphicalRenderingProperty().addListener(observable -> {
            // change rendered items if state change
            buildFactories();
        });
        getSkinnable().stringItemFormatterProperty().addListener(observable -> {
            // change rendered items if formatter change
            buildFactories();
        });
        buildFactories();

        getSkinnable().stringTextFormatterProperty().addListener(observable -> {
            // change editor converter if editor formatter change
            buildEditorRendered();
        });

        buildEditorRendered();

        // fill combo
        combo.setItems(this.items);

        // listeners
        buildListeners();

        // bindings
        buildBindings();
    }

    /**
     * build event for internal controls
     */
    private void buildEvents() {
        // combo when input text
        filterComboKeyReleased = new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                e.consume();
                if (DOWN.match(e)) {
                    if (!combo.isShowing()) {
                        combo.show();
                    }
                    return;
                } else if (e.getCode().isModifierKey() || UP.match(e) || RIGHT.match(e) || LEFT.match(e) || HOME.match(e) || END.match(e) || TAB.match(e) || e.getCode().equals(KeyCode.CONTROL) || e.isControlDown()) {
                    return;
                } else if (ESCAPE.match(e)) {
                    if (getCombo().getEditor().getCaretPosition() == 0) {
                        getCombo().getEditor().setText("");
                        control.setItem(null);
                    } else {
                        getCombo().getEditor().positionCaret(0);
                    }
                    return;
                } else if (ENTER.match(e)) {
                    validateInput();
                } else if (BACKSPACE.match(e)) {
                    if (combo.getEditor().getText().length() == 0) {
                        reScheduleSearch(e);
                    }
                } else {
                    if (!control.isAcceptFreeTextValue()) {
                        combo.valueProperty().setValue(null);
                    }
                }

                // search if possible
                if (combo.visibleProperty().getValue() && combo.getEditor().getText().length() >= control.getLimitSearch()) {
                    reScheduleSearch(e);
                    if (!combo.isShowing()) {
                        combo.show();
                    }
                }
            }
        };

        // button when pressed ENTER , ESC ...
        filterButtonKeyReleased = new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                switch (e.getCode()) {
                    case ENTER:
                        e.consume();
                        showCombo();
                        combo.getEditor().setText(userInput);
                        combo.getEditor().selectAll();
                        return;
                    case ESCAPE:
                        e.consume();
                        showCombo();
                        return;
                }

            }
        };

        // Set events ---------------------------------------------------
        combo.addEventFilter(KeyEvent.KEY_RELEASED, filterComboKeyReleased);
        combo.setOnShown(e -> reScheduleSearch(e));
        currentButton.addEventFilter(KeyEvent.KEY_RELEASED, filterButtonKeyReleased);
        currentButton.setOnAction(e -> {
                    e.consume();
                    showCombo();
                    combo.getEditor().setText(userInput);
                }
        );

    }

    private void destroyEvents() {
        combo.removeEventFilter(KeyEvent.KEY_RELEASED, filterComboKeyReleased);
        combo.setOnShown(null);
        currentButton.removeEventFilter(KeyEvent.KEY_RELEASED, filterButtonKeyReleased);
        currentButton.setOnAction(null);
    }

    private void buildListeners() {
        // loading property
        loadingIndicatorListener = o -> {
            if (!((BooleanProperty) o).getValue()) {
                combo.getEditor().positionCaret(combo.getEditor().getText().length());
            }
        };
        // item listener
        itemListener = (t, o, n) -> {
            isSelectedItem = (((ObjectProperty<T>) t).getValue() != null);
            control.refreshOnlyBean((ObservableValue<T>) t);
            refreshSkin(t);
        };
        // focus lost
        focusListener = (o, old, n) -> {
            quit = old;
            // lost focus
            if (old) {
                if (control.isControlShown()) {
                    if (quit) {
                        validateInput();
                    }
                }
            }
        };
        // managing TAB
        getFocusListener = (o, old, n) -> {
            // control has focus false - true
            if (!quit && !old) {
                showCombo();
            }
        };

        // Set bindings and listeners ---------------------------------------------------
        // when loading indicator is false. caret is put a the end of the text
        control.activityIndicatorProperty().addListener(loadingIndicatorListener);
        // T item listener
        control.itemProperty().addListener(itemListener);
        // lost focus
        combo.focusedProperty().addListener(focusListener);
        // control has focus
        control.focusedProperty().addListener(getFocusListener);
    }

    /**
     * Unbind all
     */
    private void destroyListeners() {
        control.itemProperty().removeListener(itemListener);
        control.activityIndicatorProperty().removeListener(loadingIndicatorListener);
        combo.focusedProperty().removeListener(focusListener);
        control.focusedProperty().removeListener(getFocusListener);

    }

    private void buildBindings() {
        // icone wait displayed on control load indicator value
        binder.bindInvalidationListener(iconWait.visibleProperty(), control.activityIndicatorProperty());

        // context menu
        combo.getEditor().contextMenuProperty().bind(getSkinnable().contextMenuProperty());
        combo.contextMenuProperty().bind(getSkinnable().contextMenuProperty());
        // basic control properties
        combo.editableProperty().bind(getSkinnable().editableProperty());
        combo.tooltipProperty().bind(getSkinnable().tooltipProperty());

        // combo.visibleProperty().bind(getSkinnable().visibleProperty());
    }

    /**
     * set factories
     */
    private void buildFactories() {
        if (control.isGraphicalRendering()) {
            setNodeCellFactory(control.getNodeItemFormatter());
        } else {
            setStringCellFactory(control.getStringItemFormatter());
        }
    }

    private void buildEditorRendered() {
        setTextFieldFormatter(control.getStringTextFormatter());
    }

    /**
     * Destroy factories
     */
    private void destroyFactories() {
        setNodeCellFactory(null);
        setStringCellFactory(null);
        setTextFieldFormatter(null);
    }

    public void unbindAll() {
        binder.unbindAllInvalidationListener();
        destroyListeners();
        destroyFactories();
        destroyEvents();
    }

    /**************************************************************************
     * Private Methods
     * <p>
     * ************************************************************************
     * <p>
     * /**
     * Refresh the skin when item is set
     *
     * @param item
     */
    private void refreshSkinWithItem(ObjectProperty<T> item) {
        isSelectedItem = (item.getValue() != null);
        if (item != null && item.getValue() != null) {
            userInput = control.getStringTextFormatter().apply(item.getValue());
            combo.valueProperty().setValue(item.getValue());
            if (!userInput.equalsIgnoreCase("")) {
                showButton();
            }
        } else {
            userInput = "";
            combo.valueProperty().setValue(null);
        }
    }

    /**
     * only called from control
     *
     * @param item
     */
    private void refreshSkin(Observable item) {
        ObjectProperty ob = (ObjectProperty) item;
        if (item != null) {
            T t = (T) ob.getValue();
            if (t != null && t.getValue() != null) {
                userInput = control.getStringTextFormatter().apply(t);
                currentButton.textProperty().setValue(userInput);
            } else {
                currentButton.textProperty().setValue(userInput);
            }
            combo.valueProperty().setValue(t);
        } else {
            userInput = "";
            combo.getEditor().setText("");
            currentButton.textProperty().setValue("");
            combo.valueProperty().setValue(null);
            if (!control.isControlShown()) {
                showCombo();
            }
        }
    }

    /**
     * validate the value to be displayed for each controls
     */
    private void validateInput() {
        userInput = combo.getEditor().getText();

        if (userInput.equalsIgnoreCase("")) {
            combo.getEditor().setText("");
            currentButton.textProperty().setValue("");
            combo.valueProperty().setValue(null);
            control.itemProperty().setValue(null);
            return;
        }
        // search maybe done BUT no result selected in item list (or no item list)
        if (combo.valueProperty().getValue() == null && !control.isAcceptFreeTextValue() && !control.isAutoselect()) {
            combo.getEditor().setText("");
            currentButton.textProperty().setValue("");
            combo.valueProperty().setValue(null);
            control.itemProperty().setValue(null);
            return;
        }
        // autoselect , means K code match exactly with searched word
        if (control.isAutoselect() && (combo.getSelectionModel().getSelectedItem() == null || combo.valueProperty().getValue() == null)) {
            if (combo.getItems().size() > 0) {
                T t = combo.getItems().get(0);
                if (userInput.equalsIgnoreCase(t.getKey().toString())) {
                    control.itemProperty().setValue(t);
                    showButton();
                } else {
                    combo.getEditor().setText("");
                }
            }
            return;
        }
        // free text and NO item selected
        if (control.isAcceptFreeTextValue() && (combo.getSelectionModel().getSelectedItem() == null || combo.valueProperty().getValue() == null)) {
            // TODO bug t is null
            T t = control.newInstanceOfT.apply(null);
            if (t != null && String.valueOf(t.getValue()).equalsIgnoreCase(combo.getEditor().getText())) {
                control.itemProperty().setValue(t);
                showButton();
            } else {
                combo.getEditor().setText("");
            }
            return;
        }
        // selected item
        if (combo.valueProperty().getValue() != null) {
            T t = combo.valueProperty().getValue();
            userInput = control.getStringTextFormatter().apply(t);
            if (String.valueOf(userInput).equalsIgnoreCase(combo.getEditor().getText())) {
                control.itemProperty().setValue(t);
                combo.getEditor().setText(userInput);
                showButton();
            } else {
                combo.getEditor().setText("");
            }
            return;
        }

    }

    /**
     * Called when it's needed to refresh the items list.
     * Skin does not know how the Control will do, just asking a reschedule of refresh
     *
     * @param event
     */
    private void reScheduleSearch(Event event) {
        control.reSchedule(event, control.isAlwaysRefresh());
    }

    private void graphical() {
        // building nodes
        root.setPadding(new Insets(1, 1, 1, 1));
        combo.getStylesheets().add("org/fxpart/combobox/autosuggestfx.css");
        button.add(ComponentFactory.getNewButton());
        currentButton = button.get(0);
        if (isSelectedItem) {
            exchangeNode(combo, currentButton);
            // TODO creer une fonction générique
            // ---
            combo.setVisible(false);
            combo.setMinSize(0, 0);
            combo.setMaxSize(0, 0);

            currentButton.setVisible(true);
            currentButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            currentButton.setPrefWidth(root.getWidth());
            // ---
        } else {
            exchangeNode(currentButton, combo);

            // TODO creer une fonction générique
            // ---
            currentButton.setVisible(false);
            currentButton.setMinSize(0, 0);
            currentButton.setMaxSize(0, 0);

            combo.setVisible(true);
            combo.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            combo.setPrefWidth(root.getWidth());
        }
        if (control.isMultiple()) {
            currentButton.setGraphic(iconClose);
        }
        root.getChildren().add(currentButton);
        root.getChildren().add(combo);
        root.getChildren().add(iconWait);
        getChildren().add(root);
        combo.setPromptText(control.promptTextProperty().getValue());
        combo.setEditable(control.isEditable());
    }

    private void setTextFieldFormatter(Function<T, String> textFieldFormatter) {
        combo.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T t) {
                if (t != null) {
                    String ret = textFieldFormatter.apply(t);
                }
                return t == null ? "" : textFieldFormatter.apply(t);
            }

            @Override
            public T fromString(String string) {
                return combo.getValue();
            }
        });
    }

    private void setStringCellFactory(Function<T, String> itemFormatter) {
        Callback<ListView<T>, ListCell<T>> cellFactory = new Callback<ListView<T>, ListCell<T>>() {
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
        };
        combo.setCellFactory(cellFactory);
    }

    private void setNodeCellFactory(Function<T, Node> itemFormatter) {
        combo.setCellFactory(new Callback<ListView<T>, ListCell<T>>() {
                                 @Override
                                 public ListCell<T> call(ListView<T> param) {
                                     param.setPrefHeight(getFixedHeight());
                                     final ListCell<T> cell = new ListCell<T>() {
                                         {
                                             super.setPrefWidth(control.getPrefWidth());
                                             super.setPrefHeight(control.getPrefHeight());
                                         }

                                         @Override
                                         protected void updateItem(T item, boolean empty) {
                                             super.updateItem(item, empty);
                                             if (item == null || empty) {
                                                 setGraphic(null);
                                             } else {
                                                 HBox styledText = new HBox();
                                                 String keyString = String.valueOf(item.getKey());
                                                 String valueString = String.valueOf(item.getValue());
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
//        changeParent(nodeToHide, hiddenBox);
//        changeParent(nodeToShow, visibleBox);
//        nodeToHide.maxWidth(0);
//        nodeToHide.minWidth(0);

        nodeToHide.setVisible(false);
        nodeToHide.prefWidth(1);
        nodeToHide.minWidth(1);
        nodeToHide.maxWidth(1);

//        nodeToShow.minWidth(200);
//        nodeToShow.maxWidth(200);

        nodeToShow.setVisible(true);
        nodeToHide.prefWidth(-1);
        nodeToHide.minWidth(-1);
        nodeToHide.maxWidth(-1);
    }

    /**************************************************************************
     * Public API
     **************************************************************************/

    public void showCombo() {
        control.setControlShown(new Boolean(true));
        Platform.runLater(() -> {
            exchangeNode(currentButton, combo);

            // TODO creer une fonction générique
            // ---
            currentButton.setVisible(false);
            currentButton.setMinSize(0, 0);
            currentButton.setMaxSize(0, 0);

            combo.setVisible(true);
            combo.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            combo.setPrefWidth(root.getWidth());
            // ---

            combo.getEditor().requestFocus();
            combo.getEditor().positionCaret(0);
            combo.getEditor().selectAll();
        });
    }

    public void showButton() {
        control.setControlShown(new Boolean(false));
        Platform.runLater(() -> {
            exchangeNode(combo, currentButton);

            // TODO creer une fonction générique
            // ---
            combo.setVisible(false);
            combo.setMinSize(0, 0);
            combo.setMaxSize(0, 0);

            currentButton.setVisible(true);
            currentButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            currentButton.setPrefWidth(root.getWidth());
            // ---

            currentButton.requestFocus();
        });
        currentButton.textProperty().setValue(userInput);
    }


    @Override
    public void dispose() {
        super.dispose();
        unbindAll();
    }

    /**************************************************************************
     * Public Properties
     **************************************************************************/

    public double getFixedHeight() {
        return fixedHeight.get();
    }

    public void setFixedHeight(double fixedHeight) {
        this.fixedHeight.set(fixedHeight);
    }

    public DoubleProperty fixedHeightProperty() {
        return fixedHeight;
    }

    public ComboBox<T> getCombo() {
        return combo;
    }

    public Button getButton() {
        return currentButton;
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

    public void clearAll() {
        combo.getEditor().textProperty().setValue("");
        //combo.getValue().setValue(null);
        combo.getItems().clear();
        control.getItems().clear();
        currentButton.textProperty().setValue("");
        control.itemProperty().setValue(null);
        control.beanProperty().setValue(null);
    }

    // TODO remove this when version will be 1.0.0
    public void debug(String title) {
        LOG.debug(" --- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");
        LOG.debug(" --- >>>>       " + title);
        LOG.debug(" --- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");
        LOG.debug(" --- combo Editor txt               : " + combo.getEditor().textProperty().getValue());
        LOG.debug(" --- combo T VALUE                  : " + combo.valueProperty().getValue());
        LOG.debug(" --- combo T VALUE getValue         : " + (combo.valueProperty().getValue() == null ? "" : combo.valueProperty().getValue().getValue()));
        LOG.debug(" --- combo Index selected           : " + combo.getSelectionModel().getSelectedIndex());
        LOG.debug(" ------------------------------------");
        LOG.debug(" --- button text                    : " + currentButton.textProperty().getValue());
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
        LOG.debug(" --- control  promptText            : " + control.promptTextProperty().getValue());
        LOG.debug(" --- combo  getEditor               : " + combo.getEditor().promptTextProperty().getValue());

        LOG.debug(" --- <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< ");
    }
}

