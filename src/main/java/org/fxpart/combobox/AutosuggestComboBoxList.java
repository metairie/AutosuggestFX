package org.fxpart.combobox;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
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

    private ExecutorService searchExecutor = Executors.newFixedThreadPool(1, new SearchThreadFactory());

    // TODO
    // this to be link with sub component (Combo or Table?)
    public static final EventType<Event> ON_SHOWN = new EventType<Event>(Event.ANY, "AUTOSUGGEST_ON_SHOWN");

    /**************************************************************************
     * Private fields
     **************************************************************************/

    private final ObservableList<T> items;
    private AutosuggestComboBoxListSkin skin;
    private SearchTimerTask timerTask = new SearchTimerTask();
    private Timer scheduler = new Timer();

    private String searchString = "";
    private boolean lazyMode = true;
    private boolean acceptFreeValue = false;
    private int delay = 1000; // delay in ms
    //private BooleanProperty loadingIndicator = new SimpleBooleanProperty(false);
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

    // TODO implement combo or table
    public TextField getEditor() {
        return skin.getCombo().getEditor();
    }

    public T getValue() {
        return (T) skin.getCombo().getValue();
    }

    public List<KeyValueString> getDataSource() {
        return dataSource.apply(null);
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public Function<String, List<KeyValueString>> getSearchFunction() {
        return searchFunction;
    }

    public String getSearchString() {
        return searchString;
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

    public boolean isAcceptFreeValue() {
        return acceptFreeValue;
    }

    public void setAcceptFreeValue(boolean acceptFreeValue) {
        this.acceptFreeValue = acceptFreeValue;
    }

    /**************************************************************************
     * Properties
     **************************************************************************/

    // -- On Shown
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
        dataSource = datas;
        this.textFieldFormatter = kvs -> String.format("%s", kvs.getValue());
        if (lazyMode) {
            reSchedule(null);
        }
    }


    /**************************************************************************
     * Implementation
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

    public void stopScheduler() {
        scheduler.purge();
        scheduler.cancel();
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
            skin.getSelectedItem().setDisable(true);
            stopScheduler();
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
            setOnCancelled(t -> LOG.debug(String.valueOf(getException())));
            setOnSucceeded(t -> {
                String searchString = getEditor().getText();
                ObservableList<T> list = (ObservableList<T>) getItems();
                list.setAll((Collection<? extends T>) t.getSource().getValue());
                getEditor().setText(searchString);
                setSearchString(searchString);
                if (this.event != null && KeyEvent.KEY_RELEASED == this.event.getEventType()) {
                    // TODO there is still a bug , sometimes, show does not work
                    // occurs when an item is selected and , click on button to return to the Combo
                    
                    skin.getCombo().show();
                }
                getEditor().positionCaret(searchString.length());
                skin.getSelectedItem().setDisable(false);
            });
        }

        @Override
        protected T call() throws Exception {
            return (T) getSearchFunction().apply(getEditor().getText());
        }
    }

}