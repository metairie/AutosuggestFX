package org.fxpart.combobox;

import com.google.common.collect.Lists;
import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SkinBase;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by metairie on 07-Jul-15.
 */
public class AutoSuggestFX2Skin<B> extends SkinBase<AutoSuggestFX2<B>> {
    private final static Logger LOG = LoggerFactory.getLogger(AutoSuggestFX2Skin.class);
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
    private static final KeyCodeCombination SHIFT_TAB = new KeyCodeCombination(KeyCode.TAB, KeyCodeCombination.SHIFT_DOWN);

    // apply style
    private static final String HIGHLIGHTED_DROPDOWN_CLASS = "highlighted-dropdown";
    private static final String USUAL_DROPDOWN_CLASS = "usual-dropdown";

    // visuals
    private final HBox root = new HBox();
    //private final HBox hidden = new HBox(); // for icon wait
    private final ComboBox<B> combo = new ComboBox<>();

    //private ImageView iconWait = new ImageView(new Image(getClass().getResourceAsStream("/org/fxpart/combobox/wait16.gif")));

    private String columnSeparator = "|";
    private String keyValueSeparator = " - ";

    /**************************************************************************
     * Constructors
     **************************************************************************/
    /**
     * Gather all bindings
     */
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
    public AutoSuggestFX2Skin(final AutoSuggestFX2<B> control) {
        super(control);
        this.getSkinnable().getStyleClass().add(DEFAULT_STYLE_CLASS);

        // visual aspect
        graphical();

        initSkin();
    }

    /**
     * This method is called by the constructor
     */
    private void initSkin() {

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

        combo.setItems(this.getSkinnable().items);

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
                    this.getSkinnable().searchAll();
                } else {
//                    combo.getEditor().selectAll();
                }
            }
        });

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

        combo.getEditor().addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            e.consume();

            // define ignored key
            List<KeyCode> ignoredKeyCombinations = Lists.newArrayList(KeyCode.UP, KeyCode.RIGHT, KeyCode.LEFT, KeyCode.HOME, KeyCode.END, KeyCode.TAB, KeyCode.PAGE_UP, KeyCode.PAGE_DOWN);

            // add all sub key pressed with modifier SHIFT, CONTROL
            List<KeyCodeCombination> ignoredKeyCodeCombinations = ignoredKeyCombinations.stream().flatMap(keyCombination ->
                    Lists.newArrayList(new KeyCodeCombination(keyCombination),
                            new KeyCodeCombination(keyCombination, KeyCodeCombination.SHIFT_DOWN),
                            new KeyCodeCombination(keyCombination, KeyCodeCombination.CONTROL_DOWN)).stream()).collect(Collectors.toList());

            ignoredKeyCodeCombinations.add(SHIFT_TAB);
//            if (SHIFT_TAB.match(e)) {
//                if (getSkinnable().getParent() != null && getSkinnable().getParent().getOnKeyReleased() != null) {
//                    getSkinnable().getParent().getOnKeyReleased().handle(e);
//                }
//            }

            if (e.getCode().equals(KeyCode.SHIFT) || e.getCode().equals(KeyCode.CONTROL) || ignoredKeyCodeCombinations.stream().anyMatch(keyCodeCombination -> keyCodeCombination.match(e))) {
                return;
            } else if (DOWN.match(e)) {
                return;
            } else if (SELECT_ALL.match(e)) {
                combo.getEditor().selectAll();
                return;
            } else if (ENTER.match(e) || TAB.match(e)) {
                if (combo.getEditor().getText().length() == 0) {
                    getSkinnable().setValue(null);
                }
                valid();

                return;
            } else if (PASTE.match(e)) {
                currentSearchTerm = getCombo().getEditor().getText();
                reScheduleSearch(e);
                return;
            } else if (COPY.match(e)) {
                // copy to clipboard the key and not the formatter text to prevent bad search after a copy/paste
                if (getSkinnable().getValue() != null &&
                        Objects.equals(getCombo().getEditor().getText(), getSkinnable().getStringTextFormatter().apply(getSkinnable().getValue()))) {

                    // ignore copy override if the user select a specific text
                    if (!Objects.equals(getCombo().getEditor().getSelectedText(),getCombo().getEditor().getText())) {
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(getSkinnable().getKeyTextFormatter().apply(getSkinnable().getValue()));
                        Clipboard.getSystemClipboard().setContent(content);
                    }
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
            if (combo.visibleProperty().getValue()) {
                getSkinnable().trace("SEARCH " + combo.getEditor().getText() + " " + e.getCode() + " Combo focus" + combo.isFocused() + " Editor focus " + combo.getEditor().isFocused());
                currentSearchTerm = getCombo().getEditor().getText();

                reScheduleSearch(e);
            }

        });

        // add ENTER + clear - reset value
        combo.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // validation by ENTER
            if (SHIFT_TAB.match(event)) {
                getSkinnable().trace("SHIFT TAB in COMBO");
                // do nothing
            } else if (ENTER.match(event) || TAB.match(event)) {
                if (combo.getEditor().getText().length() == 0) {
                    getSkinnable().setValue(null);
                }
                valid();
            } else if (ESCAPE.match(event)) {
                resetComboBoxEditor();
            }
        });
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
                combo.hide();
                combo.show();
            } else {
                combo.hide();
                combo.show();
            }
        }
    }

    private void resetComboBoxEditor() {
        if (getSkinnable().getValue() != null) {
            // reset the default text
            getCombo().getEditor().setText(getSkinnable().getStringTextFormatter().apply(getSkinnable().getValue()));
        } else {
            getCombo().getEditor().setText("");
        }
    }

    private void buildListeners() {
        this.getSkinnable().value.addListener((observable, oldValue, newValue) -> {
            // on value change, reset combo value
            combo.setValue(newValue);
            combo.getEditor().selectAll();
        });

        // validation when the user select value with mouse
        combo.skinProperty().addListener((observable2, oldValue2, newValue2) -> {
            ComboBoxListViewSkin skin = (ComboBoxListViewSkin) newValue2;

            ChangeListener<Boolean> detectedFocusChange = (observable1, oldValue1, newValue1) -> {
                if (!combo.focusedProperty().get() && !combo.getEditor().focusedProperty().get() && !skin.getListView().focusedProperty().get()) {
                    if (!getCombo().isShowing()) {
                        this.getSkinnable().setHasFocus(false);
                        this.resetComboBoxEditor();
                    }
                } else {
                    boolean focused = this.getSkinnable().isFocused();
                    this.getSkinnable().setHasFocus(true);

                    if (!focused && !getCombo().isShowing() && !getSkinnable().isSchedule()) {
                        // select all on gain focus
                        Platform.runLater(() -> {
                            if (!getCombo().isShowing() && !getSkinnable().isSchedule()) {
                                getCombo().getEditor().selectAll();
                            }
                        });
                    }
                }
            };
            combo.focusedProperty().addListener(detectedFocusChange);
            combo.getEditor().focusedProperty().addListener(detectedFocusChange);
            skin.getListView().focusedProperty().addListener(detectedFocusChange);


            skin.getListView().addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                if (combo.getValue() != null) {
                    valid();
                }
            });
            skin.getListView().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (SHIFT_TAB.match(event)) {
                    getSkinnable().trace("SHIFT TAB in LIST");
                    // do nothing
                } else if (ENTER.match(event) || TAB.match(event)) {
                    if (combo.getEditor().getText().length() == 0) {
                        getSkinnable().setValue(null);
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
                getSkinnable().searchAll();
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
        getSkinnable().setValue(combo.valueProperty().getValue());
    }

    private void displayValue() {
        if (combo.getValue() != null) {
            getSkinnable().trace("Item is " + getSkinnable().getStringTextFormatter().apply(combo.getValue()));
        } else {
            getSkinnable().trace("Item is null");

        }
    }

    private void buildBindings() {
        this.getSkinnable().selectedItem.bind(combo.valueProperty());

        // init the control value in the combo
        combo.setValue(getSkinnable().getValue());


        // context menu
        combo.getEditor().contextMenuProperty().bind(getSkinnable().contextMenuProperty());
        combo.contextMenuProperty().bind(getSkinnable().contextMenuProperty());
        // basic control properties
        combo.editableProperty().bind(getSkinnable().editableProperty());
        combo.tooltipProperty().bind(getSkinnable().tooltipProperty());
        combo.onContextMenuRequestedProperty().bind(getSkinnable().onContextMenuRequestedProperty());
        combo.getEditor().onContextMenuRequestedProperty().bind(getSkinnable().onContextMenuRequestedProperty());
    }

    /**
     * set factories
     */
    private void buildFactories() {
        initCellFactory();
    }

    private void buildEditorRendered() {
        combo.setConverter(new StringConverter<B>() {
            @Override
            public String toString(B t) {
                return t == null ? "" : getSkinnable().getStringTextFormatter().apply(t);
            }

            @Override
            public B fromString(String string) {
                return combo.getValue();
            }
        });
    }

    /**
     * Called when it's needed to refresh the items list.
     * Skin does not know how the Control will do, just asking a reschedule of refresh
     *
     * @param event
     */
    private void reScheduleSearch(Event event) {
        this.getSkinnable().reSchedule(null);
    }

    private void graphical() {
        combo.getStylesheets().add("org/fxpart/combobox/autosuggestfx.css");

        combo.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        combo.setMinSize(Double.MIN_VALUE, Double.MIN_VALUE);

        getChildren().addAll(combo);
        combo.promptTextProperty().bind(this.getSkinnable().promptTextProperty());
        combo.editableProperty().bind(this.getSkinnable().editableProperty());

        this.getSkinnable().getStyleClass().addListener((ListChangeListener<String>) c -> {
            // propagate
            while (c.next()) {
                c.getAddedSubList().stream().forEach(combo.getStyleClass()::add);
                c.getRemoved().stream().forEach(combo.getStyleClass()::remove);
            }
        });
    }

    private void initCellFactory() {
        combo.setCellFactory(new Callback<ListView<B>, ListCell<B>>() {
                                 @Override
                                 public ListCell<B> call(ListView<B> param) {
//                                     param.setPrefHeight(getFixedHeight());
                                     final ListCell<B> cell = new ListCell<B>() {
                                         {
//                                             super.setPrefWidth(this.getSkinnable().getPrefWidth());
//                                             super.setPrefHeight(this.getSkinnable().getPrefHeight());
                                         }

                                         @Override
                                         protected void updateItem(B item, boolean empty) {
                                             super.updateItem(item, empty);

                                             if (item == null || empty) {
                                                 setText(null);
                                                 setGraphic(null);
                                             } else {
                                                 HBox styledText = new HBox();
                                                 String valueString = AutoSuggestFX2Skin.this.getSkinnable().getStringItemFormatter().apply(item);
                                                 String guess = currentSearchTerm;

                                                 styledText = createStyledText(valueString, guess, styledText, true);

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

    @Override
    public void dispose() {
        super.dispose();
    }

    /**************************************************************************
     * Public Properties
     **************************************************************************/

    public ComboBox<B> getCombo() {
        return combo;
    }


    public String getColumnSeparator() {
        return columnSeparator;
    }

    public void setColumnSeparator(String columnSeparator) {
        this.columnSeparator = columnSeparator;
    }

    public String getKeyValueSeparator() {
        return keyValueSeparator;
    }

    public void setKeyValueSeparator(String keyValueSeparator) {
        this.keyValueSeparator = keyValueSeparator;
    }

    // TODO remove this when version will be 1.0.0
    public void debug(String title) {
        LOG.debug(" --- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");
        LOG.debug(" --- >>>>       " + title);
        LOG.debug(" --- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");
        LOG.debug(" --- combo Editor txt               : " + combo.getEditor().textProperty().getValue());
        LOG.debug(" --- combo T VALUE                  : " + combo.valueProperty().getValue());
        LOG.debug(" --- combo Index selected           : " + combo.getSelectionModel().getSelectedIndex());
        LOG.debug(" ------------------------------------");
        LOG.debug(" --- control T BEAN                 : " + this.getSkinnable().valueProperty().getValue());
        LOG.debug(" --- control T BEAN NOT POSSIBLE    : " + (this.getSkinnable().valueProperty().getValue() == null ? "" : this.getSkinnable().valueProperty().getValue()));
        LOG.debug(" --- control  promptText            : " + this.getSkinnable().promptTextProperty().getValue());
        LOG.debug(" --- combo  getEditor               : " + combo.getEditor().promptTextProperty().getValue());

        LOG.debug(" --- <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< ");
    }

    public void requestFocus() {
        getSkinnable().trace("Request focus");
        getCombo().getEditor().requestFocus();
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
}

