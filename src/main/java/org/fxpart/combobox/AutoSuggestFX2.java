package org.fxpart.combobox;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import org.fxpart.common.util.AutosuggestFXTimer;
import org.fxpart.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.TimerTask;
import java.util.function.Function;

/**
 * Created by metairie on 07-Jul-15.
 * TODO #0 getSkinControl() could generate null exception, if called before skin is created.
 * TODO #0 Replace by a Callback to ensure endControlInitialization() is done
 */
public class AutoSuggestFX2<B> extends Control {
    private final static Logger LOG = LoggerFactory.getLogger(AutoSuggestFX2.class);
    /**************************************************************************
     * Public Properties
     **************************************************************************/

    // configuration       -----------------------
    protected IntegerProperty delay = new SimpleIntegerProperty(this, "delay", 100);                        // delay in ms, max 5000
    protected int visibleRowsCount = 10;              // display only 10 items

    protected BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true);                // combo is editable
    protected BooleanProperty graphicalRendering = new SimpleBooleanProperty(this, "graphicalRendering", true);      // use for combo : Node cell factory (true) or String cell factory

    protected StringProperty promptText = new SimpleStringProperty(this, "promptText", "");

    // search from a source which is dynamic (database, ws, ...)
    protected ObjectProperty<Function<String, List<B>>> search = new SimpleObjectProperty<>(this, "search");

    // formatter        -----------------------
    protected ObjectProperty<Function<B, String>> stringTextFormatter = new SimpleObjectProperty<>(this, "stringTextFormatter");
    protected ObjectProperty<Function<B, String>> stringItemFormatter = new SimpleObjectProperty<>(this, "stringItemFormatter");

    protected ObjectProperty<B> value = new SimpleObjectProperty<>(this, "value");

    protected BooleanProperty logDebug = new SimpleBooleanProperty(this, "debug", false);
    protected Function<B, String> keyTextFormatter;
    protected ListProperty<B> items = new SimpleListProperty<>(this, "items", FXCollections.observableArrayList());
    protected ObjectProperty<B> selectedItem = new SimpleObjectProperty<>(this, "selectedItem");
    SearchTimerTask searchTask = null;
    /**************************************************************************
     * Private Properties
     **************************************************************************/

    // 1 SearchThread       -----------------------
    //private static ExecutorService executorSearch = Executors.newFixedThreadPool(1, new SearchThreadFactory());
    private AutosuggestFXTimer scheduler = null;//AutosuggestFXTimer.getInstance();

    /**
     * Creates a new AutoSuggest2
     */
    public AutoSuggestFX2() {
        editable.addListener((observable, oldValue, newValue) -> {
            if (oldValue) {
                this.setDisable(true);
            } else {
                this.setHasFocus(false);
                this.setDisable(false);
            }
        });
        Version.getInstance();
    }

    public B getSelectedItem() {
        return selectedItem.get();
    }

    public boolean getLogDebug() {
        return logDebug.get();
    }

    /**************************************************************************
     * Constructors
     **************************************************************************/

    public BooleanProperty logDebugProperty() {
        return logDebug;
    }

    /**
     * Call a load if necessary
     */
    public void start() {
        if (!(selectedItem.getValue() == null && valueProperty().getValue() == null)) {
            reSchedule();
        }
    }

    public void applyList(String term, List<B> newList) {
        if (Objects.equals(term, getSkinControl().getCombo().getEditor().getText()) || term.isEmpty()) {
            applyList(newList);
        }
    }

    /**
     * Method for doing the update of cells
     *
     * @param newList
     */
    public void applyList(List<B> newList) {
        if (newList == null) {
            return;
        }
        // set the combo item list in one time
        String searchTerm = getSkinControl().getCombo().getEditor().getText();

        items.setAll(newList);

        // prevent bug when a key type when a select item has set the editor text
        // because the item is refresh when the list is clear or replace by setAll
        if (!getSkinControl().getCombo().getEditor().getText().equals(searchTerm)) {
            getSkinControl().getCombo().getEditor().setText(searchTerm);
            getSkinControl().getCombo().getEditor().selectPositionCaret(searchTerm.length());
            getSkinControl().getCombo().getEditor().deselect();
        }

        getSkinControl().show();
    }

    public void appendResult(String finalTerm, List<B> newList) {
        // prevent loading bad term result
        String currentSearchTerm = getSkinControl().getCombo().getEditor().getText();
        if (Objects.equals(finalTerm, currentSearchTerm)) {
            if (!newList.isEmpty()) {
                items.addAll(newList);
                trace("---- Append result " + newList.size());
                getSkinControl().show();
            }
        } else {
            trace(String.format("APPEND RESULT term not corresponding : ORIG %s != %s ", currentSearchTerm, finalTerm));
        }
    }

    protected void trace(String trace) {
        if (getLogDebug()) {
            System.out.println(trace);
        }
    }

    /**
     * reSchedule a searching or a filtering task
     */
    protected void reSchedule() {
        scheduler = AutosuggestFXTimer.getInstance();

        if (searchTask != null){
            searchTask.cancel();
        }

        searchTask = new SearchTimerTask(getEditorText());

        if (getEditorText().length() < 2){
            scheduler.schedule(searchTask, 200);
        }else {
            scheduler.schedule(searchTask, this.delay.getValue());
        }
    }

    /**
     * reSchedule a searching or a filtering task
     */
    protected void searchAll() {
        scheduler = AutosuggestFXTimer.getInstance();

        SearchTimerTask searchTask = new SearchTimerTask("");
        scheduler.schedule(searchTask, this.delay.getValue());
    }


    /**************************************************************************
     * Private Methods
     **************************************************************************/

    private void stopScheduler() {
        dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Skin<?> createDefaultSkin() {
        return new AutoSuggestFX2Skin<>(this);
    }


    /**************************************************************************
     *
     * Public API
     *
     **************************************************************************/

    /**
     * release timer, executor, bindings
     */
    public void dispose() {
        if (scheduler != null) {
            scheduler.cancel();
            scheduler = null;
        }
    }

    /**
     * clear all objects. AutosuggestFx is like a new
     */
    public void clearAll() {
        setValue(null);
    }

    /**************************************************************************
     * Public Properties
     **************************************************************************/

    public AutoSuggestFX2Skin<B> getSkinControl() {
        return (AutoSuggestFX2Skin) getSkin();
    }

    public String getEditorText() {
        if (getSkinControl() == null) {
            return "";
        } else {
            return getSkinControl().getCombo().getEditor().getText();
        }
    }

    public void setEditorText(String text) {
        getSkinControl().getCombo().getEditor().setText(text);
    }

    public int getDelay() {
        return delay.getValue();
    }

    public void setDelay(int delay) {
        if (delay == 0 || delay == -1) {
            this.delay.setValue(1);
        } else {
            this.delay.setValue(Math.max(100, Math.min(5000, delay)));
        }
    }

    public boolean isEditable() {
        return editable.getValue();
    }

    public void setEditable(boolean editable) {
        this.editable.setValue(editable);
    }

    public BooleanProperty editableProperty() {
        return editable;
    }

    public Function<B, String> getStringTextFormatter() {
        return stringTextFormatter.getValue();
    }

    public void setStringTextFormatter(Function<B, String> stringTextFormatter) {
        this.stringTextFormatter.setValue(stringTextFormatter);
    }

    public ObjectProperty<Function<B, String>> stringTextFormatterProperty() {
        return stringTextFormatter;
    }

    public Function<B, String> getStringItemFormatter() {
        return stringItemFormatter.getValue();
    }

    public void setStringItemFormatter(Function<B, String> stringItemFormatter) {
        this.stringItemFormatter.setValue(stringItemFormatter);
    }

    public ObjectProperty<Function<B, String>> stringItemFormatterProperty() {
        return stringItemFormatter;
    }

    public String getColumnSeparator() {
        return getSkinControl().getColumnSeparator();
    }

    public void setColumnSeparator(String columnSeparator) {
        getSkinControl().setColumnSeparator(columnSeparator);
    }

    public String getKeyValueSeparator() {
        return getSkinControl().getKeyValueSeparator();
    }

    public void setKeyValueSeparator(String keyValueSeparator) {
        getSkinControl().setKeyValueSeparator(keyValueSeparator);
    }

    public ObjectProperty<Function<String, List<B>>> searchProperty() {
        return search;
    }

    public ObjectProperty<B> valueProperty() {
        return value;
    }

    public final B getValue() {
        return valueProperty().get();
    }

    public void setValue(B value) {
        this.value.set(value);
    }

    public int getVisibleRowsCount() {
        return visibleRowsCount;
    }

    public void setVisibleRowsCount(int visibleRowsCount) {
        this.visibleRowsCount = visibleRowsCount;
    }

    public boolean isGraphicalRendering() {
        return graphicalRendering.getValue();
    }

    public void setGraphicalRendering(boolean graphicalRendering) {
        this.graphicalRendering.setValue(graphicalRendering);
    }

    public BooleanProperty graphicalRenderingProperty() {
        return graphicalRendering;
    }

    public String getPromptText() {
        return promptText.get();
    }

    public void setPromptText(String promptText) {
        this.promptText.set(promptText);
    }

    public StringProperty promptTextProperty() {
        return promptText;
    }

    public Function<B, String> getKeyTextFormatter() {
        return keyTextFormatter;
    }

    public void setKeyTextFormatter(Function<B, String> keyTextFormatter) {
        this.keyTextFormatter = keyTextFormatter;
    }

    @Override
    public void requestFocus() {
        AutoSuggestFX2Skin skin = (AutoSuggestFX2Skin) getSkin();
        if (skin != null) {
            skin.requestFocus();
            setHasFocus(true);
        }
    }

    public void setHasFocus(Boolean focus) {
        setFocused(focus);
    }

    public ObservableList<String> getEditorStyleClass() {
        return this.getStyleClass();
    }

    protected Function<String, List<B>> getSearch() {
        return search.getValue();
    }

    public void setSearch(Function<String, List<B>> search) {
        this.search.set(search);
    }

    /**
     * If no items loaded - search all
     * otherwise show last result
     */
    public void show() {
        if (items.isEmpty()) {
            searchAll();
        } else {
            getSkinControl().getCombo().show();
        }
    }

    /**
     * inner Class for external search
     */
    public class SearchTimerTask extends TimerTask {

        private String term;

        SearchTimerTask(String term) {
            this.term = term;
        }

        @Override
        public void run() {
            final SearchTask task = new SearchTask(term);
            final Service searchService = new Service() {
                @Override
                protected Task createTask() {
                    return task;
                }
            };

            searchService.start();
        }
    }

    public class SearchTask extends Task<List<B>> {


        private String term;

        SearchTask(String term) {
            this.term = term;
            setOnCancelled(t -> {
                LOG.debug(String.valueOf(getException()));
            });
            setOnSucceeded(t -> {
                searchTask = null;
            });
            setOnFailed(t -> {

                LOG.debug(String.valueOf(getException()));
            });
        }

        @Override
        protected List<B> call() throws Exception {
            return search.getValue().apply(term);
        }
    }
}