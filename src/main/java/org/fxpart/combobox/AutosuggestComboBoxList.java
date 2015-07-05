package org.fxpart.combobox;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Pavlo on 30.06.2015.
 */
public class AutosuggestComboBoxList<T> extends ComboBox<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AutosuggestComboBoxList.class);

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

    private String searchString="";

    private Function<String, List<KeyValueString>> searchFunction;
    private Function<String, List<KeyValueString>> dataSource;

    /**
     * Lazy mode on startup lazyMode
     */
    private boolean lazyMode = true;

    /**
     * Display load indicator @param loadingIndicator
     */
    private boolean loadingIndicator = false;

    /**
     * Delay(ms) before autosuggest search start
     * @param timer
     */
    Integer timer=1000;
    private Boolean waitFlag;

    public void setSearchString(String searchString){
        this.searchString = searchString;
    }

    public Function<String, List<KeyValueString>> getSearchFunction(){
        return searchFunction;
    }

    protected Boolean getWaitFlag() {
        return waitFlag;
    }

    protected void setWaitFlag(Boolean waitFlag) {
        this.waitFlag = waitFlag;
    }

    public Integer getTimer() {
        return timer;
    }

    public void setTimer(Integer timer) {
        this.timer = timer;
    }

    public void setLazyMode(boolean lazyMode){
        this.lazyMode = lazyMode;
    }

    public boolean getLazyMode(){
        return lazyMode;
    }

    public boolean setLoadingIndicator() {
        return loadingIndicator;
    }

    public void setLoadingIndicator(boolean loadingIndicator) {
        this.loadingIndicator = loadingIndicator;
    }

    public AutosuggestComboBoxList() {
        setEditable(true);
    }

    public void setDataSource(Function<String, List<KeyValueString>> dataSource) {
        this.dataSource = dataSource;
    }

    public List<KeyValueString> getDataSource() {
        return dataSource.apply(null);
    }

    public void init(Function<String, List<KeyValueString>> datas, Function<T, String> textFieldFormatter) {
        setVisibleRowCount(10);
        setCustomCellFactory();
        addEventHandler(KeyEvent.KEY_PRESSED, t -> hide());
        addEventHandler(KeyEvent.KEY_RELEASED, createKeyReleaseEventHandler());
        dataSource = datas;
        searchFunction = term -> getDataSource().stream().filter(item -> item.getValue().contains(term == null ? "" : term)).collect(Collectors.toList());
        setTextFieldFormatter(textFieldFormatter);
        ObservableList<T> list = null;
        if (lazyMode == false) {
            list = FXCollections.observableArrayList((Collection<? extends T>) searchFunction.apply(null));
        }else{
            list = FXCollections.observableArrayList();
        }
        setItems(list);
        waitFlag = true;
    }

    protected EventHandler<KeyEvent> createKeyReleaseEventHandler() {
        return new EventHandler<KeyEvent>() {
            private boolean moveCaretToPos = false;
            private int caretPos;

            @Override
            public void handle(KeyEvent event) {
                String term = getEditor().getText();
                int termLength = 0;

                if (term != null) {
                    termLength = term.length();
                }

                if (UP.match(event)) {
                    caretPos = -1;
                    moveCaret(termLength);
                    return;
                } else if (DOWN.match(event)) {
                    if (!isShowing()) {
                       show();
                    }
                    caretPos = -1;
                    moveCaret(termLength);
                    return;
                } else if (BACK_SPACE.match(event)) {
                    moveCaretToPos = true;
                    caretPos = getEditor().getCaretPosition();
                } else if (DELETE.match(event)) {
                    moveCaretToPos = true;
                    caretPos = getEditor().getCaretPosition();
                }

                if (RIGHT.match(event) || LEFT.match(event) || event.isControlDown() || HOME.match(event) || END.match(event) || TAB.match(event)) {
                    return;
                }

                if (getValue() != null) {
                    setWaitFlag(true);
                }

                DelayedSearchTask delayedSearchTask = new DelayedSearchTask(
                        AutosuggestComboBoxList.this, timer);
                Thread delayedSearchThread = new Thread(delayedSearchTask);
                delayedSearchThread.start();

                if (!moveCaretToPos) {
                    caretPos = -1;
                }
                moveCaret(termLength);
            }

            private void moveCaret(int textLength) {
                if (caretPos == -1) {
                    getEditor().positionCaret(textLength);
                } else {
                    getEditor().positionCaret(caretPos);
                }
                moveCaretToPos = false;
            }
        };
    }

    private void setCustomCellFactory() {
        setCellFactory(new Callback<ListView<T>, ListCell<T>>() {
                           @Override
                           public ListCell<T> call(ListView<T> param) {

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
                                           if (searchString.length() != 0) {
                                               Integer searchStringPosition = valueString.indexOf(searchString);

                                               // itemString contains searchString. It should be split and searchString should be highLighted
                                               if (searchStringPosition >= 0) {
                                                   String beginString = valueString.substring(0, searchStringPosition);
                                                   String highlightedString = valueString.substring(searchStringPosition, searchStringPosition + searchString.length());
                                                   String endString = valueString.substring(searchStringPosition + searchString.length());

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

    private void setTextFieldFormatter(Function<T, String> textFieldFormatter) {
        super.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T t) {
                return t == null ? null : textFieldFormatter.apply(t);
            }

            @Override
            public T fromString(String string) {
                return getValue();
            }
        });
    }
}