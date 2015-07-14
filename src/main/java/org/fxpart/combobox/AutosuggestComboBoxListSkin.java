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
import javafx.scene.layout.VBox;
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
    private static final KeyCodeCombination ENTER = new KeyCodeCombination(KeyCode.ENTER);

    private static final String HIGHLIGHTED_CLASS = "highlighted-dropdown";
    private static final String USUAL_CLASS = "usual-dropdown";

    // visuals
    private final HBox root = new HBox();
    private final VBox vBoxCombo = new VBox();
    private final ComboBox<T> combo = new ComboBox<>();
    private final Button selectedItem = new Button();
    private final ProgressBar progressBar = new ProgressBar();
    private final VBox hiddenNode = new VBox();

    // data
    private final AutosuggestComboBoxList<T> control;
    private final ObservableList<T> items;

    // TODO qualify this properties (data?visual?control?)
    private BooleanProperty loadingIndicator = new SimpleBooleanProperty(false);
    private DoubleProperty fixedHeight = new SimpleDoubleProperty(150);
    private int visibleRowsCount = 10;
    private boolean editable = true;


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
        combo.setEditable(editable);
        combo.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ENTER:
                    if (!getCombo().getEditor().textProperty().get().equalsIgnoreCase("")) {
                        switchNode(combo, selectedItem);
                    }
                    e.consume();
            }
        });
        combo.addEventHandler(KeyEvent.KEY_RELEASED, createKeyReleaseEventHandler());

        combo.setOnShown(event -> {
            if (!selectedItem.disabledProperty().getValue()) {
                reSchedule(event);
            }
        });

        selectedItem.setOnAction(event -> {
            switchNode(selectedItem, combo);
        });

        setCustomCellFactory();
        setTextFieldFormatter(control.getTextFieldFormatter());
        combo.setItems(this.items);
    }

    private void reSchedule(Event event) {
        control.reSchedule(event);
    }

    private void graphical() {
        // building nodes
        root.setPadding(new Insets(1, 1, 1, 1));
        vBoxCombo.setPadding(new Insets(1, 1, 1, 1));
        progressBar.setMaxWidth(Double.MAX_VALUE);
        selectedItem.setMaxHeight(Double.MAX_VALUE);
        Image image = new Image(getClass().getResourceAsStream("/org/fxpart/close.png"));
        selectedItem.setContentDisplay(ContentDisplay.RIGHT);
        selectedItem.setAlignment(Pos.BASELINE_RIGHT);
        selectedItem.setPadding(new Insets(1, 5, 1, 5));
        selectedItem.setGraphic(new ImageView(image));
        vBoxCombo.getChildren().add(progressBar);
        vBoxCombo.getChildren().add(combo);
        hiddenNode.getChildren().add(selectedItem);
        root.getChildren().add(vBoxCombo);
        getChildren().add(root);
    }

    private void bind() {
        progressBar.visibleProperty().bind(loadingIndicator);
        selectedItem.textProperty().bind(combo.getEditor().textProperty());
    }

    private EventHandler<KeyEvent> createKeyReleaseEventHandler() {
        return new EventHandler<KeyEvent>() {
            private boolean moveCaretToPos = false;
            private int caretPos;

            @Override
            public void handle(KeyEvent event) {
                // nothing to do on this
                if (RIGHT.match(event) || LEFT.match(event) || event.isControlDown() || HOME.match(event) || END.match(event) || TAB.match(event)) {
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
                if (!selectedItem.disabledProperty().getValue()) {
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
                                     param.setPrefHeight(getFixedHeight());
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
        changeParent(nodeToHide, hiddenNode);
        changeParent(nodeToShow, vBoxCombo);
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

    public boolean getLoadingIndicator() {
        return loadingIndicator.get();
    }

    public BooleanProperty loadingIndicatorProperty() {
        return loadingIndicator;
    }

    public void setLoadingIndicator(boolean loadingIndicator) {
        this.loadingIndicator.set(loadingIndicator);
    }

    public ComboBox<T> getCombo() {
        return combo;
    }

    public Button getSelectedItem() {
        return selectedItem;
    }

}
