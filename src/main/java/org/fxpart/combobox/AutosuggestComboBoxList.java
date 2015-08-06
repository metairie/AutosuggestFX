package org.fxpart.combobox;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by metairie on 07-Jul-15.
 */
public class AutosuggestComboBoxList<T> extends AutosuggestControl {
    private final static Logger LOG = LoggerFactory.getLogger(AutosuggestComboBoxList.class);
    public static final EventType<Event> ON_SHOWN = new EventType<>(Event.ANY, "AUTOSUGGEST_ON_SHOWN");

    public enum STATUS_ITEM {
        EMPTY,
        SEARCH,
        FREE_TEXT_ITEM,
        COMBO_TEXT_ITEM
    }

    public enum STATUS_SKIN {
        CONTROL_VISIBLE,
        BUTTON_VISIBLE
    }

    public enum STATUS_SEARCH {
        NOTHING,
        RUN,
        SUCCESS,
        FAIL
    }

    /**************************************************************************
     * Private Properties
     **************************************************************************/

    private ExecutorService searchExecutor = Executors.newFixedThreadPool(1, new SearchThreadFactory());
    private final ObservableList<T> items;
    private AutosuggestComboBoxListSkin skin;
    private SearchTimerTask timerTask = new SearchTimerTask();
    private Timer scheduler = new Timer();

    /**************************************************************************
     * Public Properties
     **************************************************************************/

    private boolean lazyMode = true;
    private boolean acceptFreeTextValue = false;
    private int delay = 1000; // delay in ms
    private int visibleRowsCount = 10;
    private boolean editable = true;
    private BooleanProperty loadingIndicator = new SimpleBooleanProperty(false);
    private StringProperty searchStatus = new SimpleStringProperty(String.valueOf(STATUS_SEARCH.NOTHING));
    private StringProperty skinStatus = new SimpleStringProperty(String.valueOf(STATUS_SKIN.CONTROL_VISIBLE));
    private Function<String, List<KeyValueString>> searchFunction = (term -> getDataSource().stream().filter(item -> item.getValue().contains(term == null ? "" : term)).collect(Collectors.toList()));
    private Function<String, List<KeyValueString>> dataSource = s -> null;
    private Function<KeyValueString, String> textFieldFormatter = item -> String.format("%s", item.getValue());
    private Function<KeyValueString, String> labelItemFormatter = item -> String.format("%s - %s", item.getKey(), item.getValue());


    /**************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    /**
     * Creates a new AutosuggestComboBoxList instance with an empty list of choices.
     */
    public AutosuggestComboBoxList() {
        this(null);
    }

    /**
     * Creates a new AutosuggestComboBoxList instance
     *
     * @param items The items to display.
     */
    public AutosuggestComboBoxList(final ObservableList<T> items) {
        final int initialSize = items == null ? 32 : items.size();
        this.items = items == null ? FXCollections.<T>observableArrayList() : items;
    }


    /**************************************************************************
     *
     * Public API
     *
     **************************************************************************/

    /**
     * {@inheritDoc}
     */
    @Override
    protected Skin<?> createDefaultSkin() {
        skin = new AutosuggestComboBoxListSkin<>(this);
        return skin;
    }

    /**
     * Represents the list of items.
     */
    public ObservableList<T> getItems() {
        return items;
    }

    public void setEditorText(String text) {
        skin.getCombo().getEditor().setText(text);
    }

    public String getEditorText() {
        return skin.getCombo().getEditor().getText();
    }

    /**************************************************************************
     * Public Properties
     **************************************************************************/

    public List<KeyValueString> getDataSource() {
        return dataSource.apply(null);
    }

    public void setDataSource(Function<String, List<KeyValueString>> dataSource) {
        this.dataSource = dataSource;
    }

    public Function<String, List<KeyValueString>> getSearchFunction() {
        return searchFunction;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = Math.max(100, Math.min(5000, delay));
    }

    public Function<KeyValueString, String> getTextFieldFormatter() {
        return textFieldFormatter;
    }

    public void setTextFieldFormatter(Function<KeyValueString, String> textFieldFormatter) {
        this.textFieldFormatter = textFieldFormatter;
    }

    public Function<KeyValueString, String> getLabelItemFormatter() {
        return labelItemFormatter;
    }

    public void setLabelItemFormatter(Function<KeyValueString, String> labelItemFormatter) {
        this.labelItemFormatter = labelItemFormatter;
    }

    public void setLazyMode(boolean lazyMode) {
        this.lazyMode = lazyMode;
    }

    public boolean getLazyMode() {
        return lazyMode;
    }

    public boolean isLazyMode() {
        return lazyMode;
    }

    public boolean isAcceptFreeTextValue() {
        return acceptFreeTextValue;
    }

    public void setAcceptFreeTextValue(boolean acceptFreeTextValue) {
        this.acceptFreeTextValue = acceptFreeTextValue;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public String getSearchStatus() {
        return searchStatus.get();
    }

    public StringProperty searchStatusProperty() {
        return searchStatus;
    }

    public void setSearchStatus(String searchStatus) {
        this.searchStatus.set(searchStatus);
    }

    public String getSkinStatus() {
        return skinStatus.get();
    }

    public StringProperty skinStatusProperty() {
        return skinStatus;
    }

    public void setSkinStatus(String skinStatus) {
        this.skinStatus.set(skinStatus);
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

    public final T getValue() {
        return (T) skin.getCombo().getValue();
    }

    // ----------------------------------------------------------------------- On Shown
    public final ObjectProperty<EventHandler<Event>> onShownProperty() {
        return onShown;
    }

    public final void setOnShown(EventHandler<Event> value) {
        onShownProperty().set(value);
    }

    public final EventHandler<Event> getOnShown() {
        return onShownProperty().get();
    }

    /**
     * Called just after the {@link AutosuggestControl} popup/display is shown.
     */
    private ObjectProperty<EventHandler<Event>> onShown = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override
        protected void invalidated() {
            setEventHandler(ON_SHOWN, get());
        }

        @Override
        public Object getBean() {
            return this;
        }

        @Override
        public String getName() {
            return "onShown";
        }
    };

    // --- converter
    private ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<StringConverter<T>>(this, "converter");

    /**
     * A {@link StringConverter} that, given an object of type T, will
     * return a String that can be used to represent the object visually.
     */
    public final ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    /**
     * Sets the {@link StringConverter} to be used in the control.
     *
     * @param value A {@link StringConverter} that, given an object of type T, will
     *              return a String that can be used to represent the object visually.
     */
    public final void setConverter(StringConverter<T> value) {
        converterProperty().set(value);
    }

    /**
     * A {@link StringConverter} that, given an object of type T, will
     * return a String that can be used to represent the object visually.
     */
    public final StringConverter<T> getConverter() {
        return converterProperty().get();
    }

    public void init(Function<String, List<KeyValueString>> datas, Function<KeyValueString, String> textFieldFormatter) {
        this.dataSource = datas;
        this.textFieldFormatter = textFieldFormatter;
        if (!lazyMode) {
            reSchedule(null);
        }
    }


    /**************************************************************************
     * Implementation, Public Methods
     **************************************************************************/

    public void reSchedule(Event event) {
        if (scheduler != null) {
            scheduler.purge();
            scheduler.cancel();
        }
        scheduler = new Timer();
        timerTask = new SearchTimerTask(event);
        // running timer task as daemon thread
        scheduler.schedule(timerTask, this.delay, this.delay);
    }

    public class SearchTimerTask extends TimerTask {
        SearchTimerTask() {
            this(null);
        }

        SearchTimerTask(Event event) {
            this.event = event;
        }

        private Event event;

        @Override
        public void run() {
            startSearch();
            SearchTask<T> searchTask = new SearchTask<>(this.event);
            searchExecutor.submit(searchTask);
        }
    }

    public class SearchTask<T> extends Task<T> {
        private Event event;

        SearchTask() {
            this(null);
        }

        SearchTask(Event event) {
            this.event = event;
            setOnCancelled(t -> {
                searchStatus.setValue(String.valueOf(STATUS_SEARCH.FAIL));
                stopSearch();
                LOG.debug(String.valueOf(getException()));
            });
            setOnSucceeded(t -> {
                searchStatus.setValue(String.valueOf(STATUS_SEARCH.SUCCESS));
                ObservableList<T> list = (ObservableList<T>) getItems();
                list.setAll((Collection<? extends T>) t.getSource().getValue());
                stopSearch();
                if (this.event != null && KeyEvent.KEY_RELEASED == this.event.getEventType() && checkEnumProperty(skinStatusProperty(), STATUS_SKIN.CONTROL_VISIBLE)) {
                    skin.getCombo().show();
                }
            });
        }

        @Override
        protected T call() throws Exception {
            // TODO remove on production
            // Thread.sleep(500);
            return (T) getSearchFunction().apply(getEditorText());
        }
    }

    public void startSearch() {
        setLoadingIndicator(true);
        searchStatus.setValue(String.valueOf(STATUS_SEARCH.RUN));
        stopScheduler();
    }

    public void stopSearch() {
        stopScheduler();
        skin.getCombo().getEditor().positionCaret(getEditorText().length());
        setLoadingIndicator(false);
    }

    /**************************************************************************
     * Private Methods
     **************************************************************************/

    private void stopScheduler() {
        scheduler.purge();
        scheduler.cancel();
    }

    private boolean checkEnumProperty(StringProperty p, Enum e) {
        return p.getValue().equalsIgnoreCase(String.valueOf(e));
    }
}