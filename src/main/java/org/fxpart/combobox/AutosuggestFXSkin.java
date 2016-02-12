package org.fxpart.combobox;

import com.google.common.collect.Lists;
import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.*;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by metairie on 07-Jul-15.
 */
public class AutosuggestFXSkin<B, T extends KeyValue> extends SkinBase<AutosuggestFX<B, T>> {
    private final static Logger LOG = LoggerFactory.getLogger(AutosuggestFXSkin.class);
    private static final String DEFAULT_STYLE_CLASS = "autosuggestfx";

    /**************************************************************************
     * fields
     **************************************************************************/
    private static final KeyCodeCombination ENTER = new KeyCodeCombination(KeyCode.ENTER);
    private static final KeyCodeCombination BACKSPACE = new KeyCodeCombination(KeyCode.BACK_SPACE);
    private static final KeyCodeCombination ESCAPE = new KeyCodeCombination(KeyCode.ESCAPE);
    private static final KeyCodeCombination DELETE = new KeyCodeCombination(KeyCode.DELETE);
    //    private static final KeyCodeCombination UP = new KeyCodeCombination(KeyCode.UP);
    private static final KeyCodeCombination DOWN = new KeyCodeCombination(KeyCode.DOWN);
    //    private static final KeyCodeCombination PAGE_DOWN = new KeyCodeCombination(KeyCode.PAGE_DOWN);
//    private static final KeyCodeCombination PAGE_UP = new KeyCodeCombination(KeyCode.PAGE_UP);
//    private static final KeyCodeCombination LEFT = new KeyCodeCombination(KeyCode.LEFT);
//    private static final KeyCodeCombination RIGHT = new KeyCodeCombination(KeyCode.RIGHT);
//    private static final KeyCodeCombination HOME = new KeyCodeCombination(KeyCode.HOME);
    private static final KeyCodeCombination TAB = new KeyCodeCombination(KeyCode.TAB);
    //    private static final KeyCodeCombination END = new KeyCodeCombination(KeyCode.END);
    private static final KeyCodeCombination PASTE = new KeyCodeCombination(KeyCode.V, KeyCodeCombination.CONTROL_DOWN);
    private static final KeyCodeCombination SELECT_ALL = new KeyCodeCombination(KeyCode.A, KeyCodeCombination.CONTROL_DOWN);
    private static final KeyCodeCombination COPY = new KeyCodeCombination(KeyCode.C, KeyCodeCombination.CONTROL_DOWN);

    // apply style
    private static final String HIGHLIGHTED_DROPDOWN_CLASS = "highlighted-dropdown";
    private static final String USUAL_DROPDOWN_CLASS = "usual-dropdown";

    // visuals
    private final HBox root = new HBox();
    //private final HBox hidden = new HBox(); // for icon wait
    private final ComboBox<T> combo = new ComboBox<>();
    private final List<Button> button = new ArrayList<>();

    // listeners & binds, events
    private final WeakBinder binder = new WeakBinder();
    private Button currentButton = null;

    private DoubleProperty fixedHeight = new SimpleDoubleProperty(150);
    private boolean columnSeparatorVisible = false;
    //private ImageView iconWait = new ImageView(new Image(getClass().getResourceAsStream("/org/fxpart/combobox/wait16.gif")));
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
    private String currentSearchTerm = "";

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
        super(control);
        this.getSkinnable().getStyleClass().add(DEFAULT_STYLE_CLASS);

        // visual aspect
        graphical();

        initSkin();

        if (control.isRefreshFXML()) {
            refreshSkinWithItem(control.itemProperty());
        }

        this.getSkinnable().focusedProperty().addListener((observable, oldValue, newValue) -> {
            getSkinnable().setHasFocus(newValue);
        });
    }

    /**
     * Constructor with an T Item passed
     * </p>
     * this.getSkinnable().item is already feeded by developer
     * Possible usage :
     * - when you want to display an Entity Form with data retrieved from a service
     *
     * @param control
     * @param item
     */
    public AutosuggestFXSkin(final AutosuggestFX<B, T> control, ObjectProperty<T> item) {
        super(control);

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
        this.items = this.getSkinnable().getItems();

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
        combo.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {

                if (!combo.getEditor().isFocused()) {
                    List<T> result = getSkinnable().getSearch().apply("");
                    if (result != null) {
                        getSkinnable().applyList(result);
                    }
                } else {
                    combo.getEditor().selectAll();
                }
            }
        });

        // combo when input text
        filterComboKeyReleased = new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                e.consume();
                // define ignored key
                List<KeyCode> ignoredKeyCombinations = Lists.newArrayList(KeyCode.UP, KeyCode.RIGHT, KeyCode.LEFT, KeyCode.HOME, KeyCode.END, KeyCode.TAB, KeyCode.PAGE_UP, KeyCode.PAGE_DOWN);

                // add all sub key pressed with modifier SHIFT, CONTROL
                List<KeyCodeCombination> ignoredKeyCodeCombinations = ignoredKeyCombinations.stream().flatMap(keyCombination ->
                        Lists.newArrayList(new KeyCodeCombination(keyCombination),
                                new KeyCodeCombination(keyCombination, KeyCodeCombination.SHIFT_DOWN),
                                new KeyCodeCombination(keyCombination, KeyCodeCombination.CONTROL_DOWN)).stream()).collect(Collectors.toList());

                if (DOWN.match(e)) {
                    return;
                } else if (SELECT_ALL.match(e)) {
                    combo.getEditor().selectAll();
                    return;
                } else if (ENTER.match(e) || TAB.match(e)) {
                    if (combo.getEditor().getText().length() == 0) {
                        getSkinnable().setBean(null);
                    }
                    valid();
                    // go next
                    Parent parent = ((Control) e.getSource()).getParent();
                    if (parent.getParent() == null) {
                        return;
                    }
                    int i = parent.getParent().getChildrenUnmodifiable().indexOf(parent);
                    Optional<Node> first = parent.getParent().getChildrenUnmodifiable().stream().skip(i).filter(Node::isFocusTraversable).findFirst();
                    first.ifPresent(Node::requestFocus);

                    return;
                } else if (e.getCode().equals(KeyCode.SHIFT) || e.getCode().equals(KeyCode.CONTROL) || ignoredKeyCodeCombinations.stream().anyMatch(keyCodeCombination -> keyCodeCombination.match(e))) {
                    return;
                } else if (PASTE.match(e)) {
                    reScheduleSearch(e);
                    return;
                } else if (COPY.match(e)) {
                    // copy to clipboard the key and not the formatter text to prevent bad search after a copy/paste
                    if (getSkinnable().getBean() != null &&
                            Objects.equals(getCombo().getEditor().getText(), getSkinnable().getStringTextFormatter().apply(getSkinnable().getBeanToItemMapping().apply(new SimpleObjectProperty<>(getSkinnable().getBean()))))) {
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(getSkinnable().getKeyTextFormatter().apply(getSkinnable().getBeanToItemMapping().apply(new SimpleObjectProperty<>(getSkinnable().getBean()))));
                        Clipboard.getSystemClipboard().setContent(content);
                    }
                    return;
                } else if (ESCAPE.match(e)) {
                    // reset old value
                    resetComboBoxEditor();
                    return;
                } else if (DELETE.match(e)) {
                    // relaunch search all (could be a problem if the query is slow)
                } else if (BACKSPACE.match(e)) {
                    if (getCombo().getEditor().getText().isEmpty()) {
                        return;
                    }
                } else {
                    // TODO bug with isAcceptFreeTextValue
//                    if (!this.getSkinnable().isAcceptFreeTextValue()) {
//                        combo.valueProperty().setValue(null);
//                    }
                }

                // search if possible
                if (combo.visibleProperty().getValue() && combo.getEditor().getText().length() >= AutosuggestFXSkin.this.getSkinnable().getLimitSearch()) {
                    getSkinnable().trace("SEARCH " + combo.getEditor().getText() + " " + e.getCode() + " Combo focus" + combo.isFocused() + " Editor focus " + combo.getEditor().isFocused());
                    currentSearchTerm = getCombo().getEditor().getText();

                    reScheduleSearch(e);
                }
            }
        };

        // key pressed event
        combo.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            List<KeyCode> ignoredKeyCombinations = Lists.newArrayList(KeyCode.UP, KeyCode.RIGHT, KeyCode.LEFT, KeyCode.HOME, KeyCode.END, KeyCode.TAB, KeyCode.PAGE_UP, KeyCode.PAGE_DOWN);

            List<KeyCodeCombination> ignoredKeyCodeCombinations = ignoredKeyCombinations.stream().flatMap(keyCombination ->
                    Lists.newArrayList(new KeyCodeCombination(keyCombination),
                            new KeyCodeCombination(keyCombination, KeyCodeCombination.SHIFT_DOWN),
                            new KeyCodeCombination(keyCombination, KeyCodeCombination.CONTROL_DOWN)).stream()).collect(Collectors.toList());

            ignoredKeyCodeCombinations.addAll(Lists.newArrayList(DOWN, SELECT_ALL, ENTER, PASTE, COPY, ESCAPE, DELETE, ENTER, BACKSPACE));

            if (e.getCode().equals(KeyCode.SHIFT) || e.getCode().equals(KeyCode.CONTROL) || ignoredKeyCodeCombinations.stream().anyMatch(keyCodeCombination -> keyCodeCombination.match(e))) {
                return;
            }

            // detect if text editor if the selected value
            if (isEditorTextItemSelected()) {
                currentSearchTerm = "";
                // reset last search
                getCombo().getEditor().setText(currentSearchTerm);
            }


        });

        // Set events ---------------------------------------------------
        combo.getEditor().addEventFilter(KeyEvent.KEY_RELEASED, filterComboKeyReleased);

        // add ENTER + clear - reset value
        combo.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // validation by ENTER
            if (ENTER.match(event) || TAB.match(event)) {
                if (combo.getEditor().getText().length() == 0) {
                    getSkinnable().setBean(null);
                }
                valid();
            } else if (ESCAPE.match(event)) {
                resetComboBoxEditor();
            }
        });

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
                }

            }
        };

    }

    private boolean isEditorTextItemSelected() {
        return getCombo().getValue() != null && Objects.equals(getCombo().getEditor().getText(), getSkinnable().getStringTextFormatter().apply(getCombo().getValue()));
    }

    /**
     * Method show
     */
    public void show() {
        // add a sad effect but display is better
        if (!combo.isShowing()) {
            combo.show();
        }

        autoRefreshIfContentListIsNotWellDisplayed();
    }

    private void autoRefreshIfContentListIsNotWellDisplayed() {
        ComboBoxListViewSkin skin = (ComboBoxListViewSkin) getCombo().getSkin();
        final double ROW_HEIGHT = 23.3;
        double heightAll = getCombo().getItems().size() * ROW_HEIGHT;
        double heightMax = getCombo().getVisibleRowCount() * ROW_HEIGHT;
        double min = Math.min(heightMax, heightAll);
        getSkinnable().trace("Actual size " + skin.getListView().getHeight() + " min " + min);
        if (!Objects.equals(skin.getListView().getHeight(), min)) {
            getSkinnable().trace("REFRESH - size current " + skin.getListView().getHeight() + " - " + min);
            if (!combo.isShowing()) {
                combo.show();

                combo.show();
                combo.hide();
            } else {
                combo.hide();
                combo.show();
            }
        }
    }

    private void resetComboBoxEditor() {
        if (getSkinnable().getBean() != null) {
            // reset the default text
            getCombo().getEditor().setText(getSkinnable().getStringTextFormatter().apply(getSkinnable().getBeanToItemMapping().apply(new SimpleObjectProperty<>(getSkinnable().getBean()))));
        } else {
            getCombo().getEditor().setText("");
        }
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
            // nothing to to
            // change indicator status ?
        };
        // item listener
        itemListener = (t, o, n) -> {
//            isSelectedItem = (((ObjectProperty<T>) t).getValue() != null);
//           this.getSkinnable().refreshOnlyBean((ObservableValue<T>) t);
//            refreshSkin(t);
        };
        // focus lost
        focusListener = (o, old, n) -> {
            if (old) {
                getSkinnable().trace("FOCUS LOST RESET EDITOR");
                resetComboBoxEditor();
            } else {
                getSkinnable().setHasFocus(true);
            }
        };
        // managing TAB
        getFocusListener = (o, old, n) -> {
            // nothing to do
        };

        // Set bindings and listeners ---------------------------------------------------
        // when loading indicator is false. caret is put a the end of the text
        this.getSkinnable().activityIndicatorProperty().addListener(loadingIndicatorListener);
        // T item listener
        this.getSkinnable().itemProperty().addListener(itemListener);


        // lost focus
        combo.focusedProperty().addListener(focusListener);
        combo.getEditor().focusedProperty().addListener((observable3, oldValue3, newValue3) -> {
            if (newValue3) {
                getSkinnable().trace("EDITOR GAIN FOCUS DETECTED");
                getSkinnable().setHasFocus(true);
            }
        });


        // validation when the user select value with mouse
        combo.skinProperty().addListener((observable2, oldValue2, newValue2) -> {
            ComboBoxListViewSkin skin = (ComboBoxListViewSkin) newValue2;
            skin.getListView().addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                if (combo.getValue() != null) {
                    valid();
                }
            });
            skin.getListView().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (ENTER.match(event) || TAB.match(event)) {
                    if (combo.getEditor().getText().length() == 0) {
                        getSkinnable().setBean(null);
                    }
                    valid();
                } else if (ESCAPE.match(event)) {
                    resetComboBoxEditor();
                }
            });

        });

        // use when the user click on the button to search all
        combo.armedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                forceSearchAll();
            }
        });


        // when user press DOWN on the editor to show the list
        combo.getEditor().addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (DOWN.match(e)) {
                if (!combo.isShowing()) {
                    if (combo.getItems().isEmpty()) {
                        // launch a new search
                        reScheduleSearch(e);
                    } else {
                        // show already loaded result
                        show();
                    }
                }
            }
        });

        /**
         * TRACE listener
         */

        combo.valueProperty().addListener((observable1, oldValue1, newValue1) -> {
            getSkinnable().trace("Value Change !");
            displayValue();
        });

        combo.getEditor().textProperty().addListener((observable1, oldValue1, newValue1) -> {
            getSkinnable().trace("Text editor Change !");
            displayValue();
        });

    }

    private void valid() {
        displayValue();
        getSkinnable().setBean(getSkinnable().getItemToBeamMapping().apply(combo.valueProperty()));
    }

    private void displayValue() {
        if (combo.getValue() != null) {
            getSkinnable().trace("Item is " + getSkinnable().getStringTextFormatter().apply(combo.getValue()));
        } else {
            getSkinnable().trace("Item is null");

        }
    }

    private List<T> forceSearchAll() {
        return getSkinnable().getSearch().apply("");
    }

    /**
     * Unbind all
     */
    private void destroyListeners() {
        this.getSkinnable().itemProperty().removeListener(itemListener);
        this.getSkinnable().activityIndicatorProperty().removeListener(loadingIndicatorListener);
        combo.focusedProperty().removeListener(focusListener);
        this.getSkinnable().focusedProperty().removeListener(getFocusListener);
    }

    private void buildBindings() {
        // icone wait displayed on control load indicator value
        //binder.bindInvalidationListener(iconWait.visibleProperty(),this.getSkinnable().activityIndicatorProperty());

        // context menu
        combo.getEditor().contextMenuProperty().bind(getSkinnable().contextMenuProperty());
        combo.contextMenuProperty().bind(getSkinnable().contextMenuProperty());
        // basic control properties
        combo.editableProperty().bind(getSkinnable().editableProperty());
        combo.tooltipProperty().bind(getSkinnable().tooltipProperty());
    }

    /**
     * set factories
     */
    private void buildFactories() {
        if (this.getSkinnable().isGraphicalRendering()) {
            setNodeCellFactory(this.getSkinnable().getNodeItemFormatter());
        } else {
            setStringCellFactory(this.getSkinnable().getStringItemFormatter());
        }
    }

    private void buildEditorRendered() {
        setTextFieldFormatter();
    }

    /**
     * Destroy factories
     */
    private void destroyFactories() {
        setNodeCellFactory(null);
        setStringCellFactory(null);
        setTextFieldFormatter();
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
            userInput = this.getSkinnable().getStringTextFormatter().apply(item.getValue());
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
                userInput = this.getSkinnable().getStringTextFormatter().apply(t);
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
            if (!this.getSkinnable().isControlShown()) {
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
            this.getSkinnable().itemProperty().setValue(null);
            return;
        }
        // search maybe done BUT no result selected in item list (or no item list)
        if (combo.valueProperty().getValue() == null && !this.getSkinnable().isAcceptFreeTextValue() && !this.getSkinnable().isAutoselect()) {
            combo.getEditor().setText("");
            currentButton.textProperty().setValue("");
            combo.valueProperty().setValue(null);
            this.getSkinnable().itemProperty().setValue(null);
            return;
        }
        // autoselect , means K code match exactly with searched word
        if (this.getSkinnable().isAutoselect() && (combo.getSelectionModel().getSelectedItem() == null || combo.valueProperty().getValue() == null)) {
            if (combo.getItems().size() > 0) {
                T t = combo.getItems().get(0);
                if (userInput.equalsIgnoreCase(t.getKey().toString())) {
                    this.getSkinnable().itemProperty().setValue(t);
                    showButton();
                } else {
                    combo.getEditor().setText("");
                }
            }
            return;
        }
        // free text and NO item selected
        if (this.getSkinnable().isAcceptFreeTextValue() && (combo.getSelectionModel().getSelectedItem() == null || combo.valueProperty().getValue() == null)) {
            // TODO bug t is null
            T t = this.getSkinnable().newInstanceOfT.apply(null);
            if (t != null && String.valueOf(t.getValue()).equalsIgnoreCase(combo.getEditor().getText())) {
                this.getSkinnable().itemProperty().setValue(t);
                showButton();
            } else {
                combo.getEditor().setText("");
            }
            return;
        }
        // selected item
        if (combo.valueProperty().getValue() != null) {
            T t = combo.valueProperty().getValue();
            userInput = this.getSkinnable().getStringTextFormatter().apply(t);
            if (String.valueOf(userInput).equalsIgnoreCase(combo.getEditor().getText())) {
                this.getSkinnable().itemProperty().setValue(t);
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
        this.getSkinnable().reSchedule(event, this.getSkinnable().isAlwaysRefresh());
    }

    private void graphical() {


        // building nodes
//        hidden.setVisible(true);
//        hidden.setMaxSize(0, 0);
//        hidden.setPrefSize(0, 0);
//        hidden.getChildren().add(iconWait);
//        iconWait.resize(0, 0);
        //root.setPadding(new Insets(1, 1, 0, 0));

        combo.getStylesheets().add("org/fxpart/combobox/autosuggestfx.css");
        button.add(ComponentFactory.getNewButton());
        currentButton = button.get(0);
//        exchangeNode(isSelectedItem);
//        root.getChildren().addAll(currentButton, combo);
//        root.getChildren().addAll(combo);

        combo.minWidthProperty().bind(getSkinnable().minWidthProperty());
        combo.maxHeightProperty().bind(getSkinnable().maxHeightProperty());
        combo.prefWidthProperty().bind(getSkinnable().prefWidthProperty());

        getChildren().addAll(combo);
        combo.promptTextProperty().bind(this.getSkinnable().promptTextProperty());
        combo.editableProperty().bind(this.getSkinnable().editableProperty());
    }

    private void setTextFieldFormatter() {
        combo.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T t) {
                return t == null ? "" : getSkinnable().getStringTextFormatter().apply(t);
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
//                                     param.setPrefHeight(getFixedHeight());
                                     final ListCell<T> cell = new ListCell<T>() {
                                         {
//                                             super.setPrefWidth(this.getSkinnable().getPrefWidth());
//                                             super.setPrefHeight(this.getSkinnable().getPrefHeight());
                                         }

                                         @Override
                                         protected void updateItem(T item, boolean empty) {
                                             super.updateItem(item, empty);

                                             if (item == null || empty) {
                                                 setText(null);
                                                 setGraphic(null);
                                             } else {
                                                 HBox styledText = new HBox();
                                                 String keyString = getSkinnable().getKeyTextFormatter().apply(item);
                                                 String valueString = String.valueOf(item.getValue());
                                                 String guess = AutosuggestFXSkin.this.getSkinnable().getEditorText();

                                                 // key
                                                 if (AutosuggestFXSkin.this.getSkinnable().isFullSearch()) {
                                                     styledText = createStyledText(keyString, guess, styledText, AutosuggestFXSkin.this.getSkinnable().isIgnoreCase());
                                                 } else {
                                                     styledText.getChildren().add(new Text(keyString));
                                                 }

                                                 // kv separator
                                                 styledText.getChildren().add(new Text(keyValueSeparator));

                                                 // value
                                                 styledText = createStyledText(valueString, guess, styledText, AutosuggestFXSkin.this.getSkinnable().isIgnoreCase());

                                                 // render
                                                 setGraphic(styledText);
//
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
     */
    private void exchangeNode(boolean isSelectedItem) {
//        double w = root.getWidth();
//        root.setMinWidth(w);
//        if (!currentButton.isVisible()) {
//            // -- show button
//            combo.setVisible(false);
//            combo.setMinSize(0, 0);
//            combo.setMaxSize(0, 0);
//            currentButton.setVisible(true);
//            currentButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
//            currentButton.setPrefWidth(w);
//            //hidden.setVisible(false);
//           // iconWait.resize(0, 0);
//        } else {
//            // -- show combo
//            currentButton.setVisible(false);
//            currentButton.setMinSize(0, 0);
//            currentButton.setMaxSize(0, 0);
//            combo.setVisible(true);
//            combo.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
//            combo.setPrefWidth(w);
//           // hidden.setVisible(true);
//            //iconWait.resize(32, 32);
//        }
//        root.setMinWidth(-1);
    }

    /**************************************************************************
     * Public API
     **************************************************************************/

    public void showCombo() {
//        this.getSkinnable().setControlShown(new Boolean(true));
//        Platform.runLater(() -> {
//            exchangeNode(true);
//            combo.getEditor().requestFocus();
//            combo.getEditor().positionCaret(0);
//            combo.getEditor().selectAll();
//        });
    }

    public void showButton() {
//        this.getSkinnable().setControlShown(new Boolean(false));
//        Platform.runLater(() -> {
//            exchangeNode(true);
//            currentButton.requestFocus();
//        });
//        currentButton.textProperty().setValue(userInput);
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
        this.getSkinnable().getItems().clear();
        currentButton.textProperty().setValue("");
        this.getSkinnable().itemProperty().setValue(null);
        this.getSkinnable().beanProperty().setValue(null);
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
        LOG.debug(" --- control T ITEM                 : " + this.getSkinnable().itemProperty().getValue());
        LOG.debug(" --- control T ITEM getValue        : " + (this.getSkinnable().itemProperty().getValue() == null ? "" : this.getSkinnable().itemProperty().getValue().getValue()));
        LOG.debug(" --- control T ITEMS LIST           : " + this.getSkinnable().getItems().size());
        LOG.debug(" --- control  isSelected            : " + isSelectedItem);
        LOG.debug(" --- control T BEAN                 : " + this.getSkinnable().beanProperty().getValue());
        LOG.debug(" --- control T BEAN NOT POSSIBLE    : " + (this.getSkinnable().beanProperty().getValue() == null ? "" : this.getSkinnable().beanProperty().getValue()));
        LOG.debug(" --- control  isControlShown        : " + this.getSkinnable().isControlShown());
        LOG.debug(" --- control  isAcceptFreeTextValue : " + this.getSkinnable().isAcceptFreeTextValue());
        LOG.debug(" --- control  isFullSearch          : " + this.getSkinnable().isFullSearch());
        LOG.debug(" --- control  isIgnoreCase          : " + this.getSkinnable().isIgnoreCase());
        LOG.debug(" --- control  isLazyMode            : " + this.getSkinnable().isLazyMode());
        LOG.debug(" --- control  promptText            : " + this.getSkinnable().promptTextProperty().getValue());
        LOG.debug(" --- combo  getEditor               : " + combo.getEditor().promptTextProperty().getValue());

        LOG.debug(" --- <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< ");
    }

    public void requestFocus() {

        getCombo().requestFocus();
//        ComboBoxListViewSkin skin = (ComboBoxListViewSkin) getCombo().getSkin();
//        skin.getListView().requestFocus();
    }

//
//    @Override
//    protected void handleControlPropertyChanged(String propertyReference) {
//        if ("prefColumnCount".equals(propertyReference)) {
//            this.getSkinnable().requestLayout();
//        } else {
//            super.handleControlPropertyChanged(propertyReference);
//        }
//    }

    @Override
    protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return combo.maxWidth(height);
    }

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return combo.minWidth(height);
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return combo.prefWidth(height);
    }

    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return combo.minHeight(width);
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return combo.prefHeight(width);
    }

    @Override
    protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return combo.maxHeight(width);
    }

    @Override
    public double computeBaselineOffset(double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + combo.getBaselineOffset();
    }


}

