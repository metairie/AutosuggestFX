package org.fxpart.combobox;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import org.fxpart.version.Version;
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
public class AutosuggestComboBoxList<B, T extends KeyValue> extends AutosuggestControl {

    private final static Logger LOG = LoggerFactory.getLogger(AutosuggestComboBoxList.class);
    public static final EventType<Event> ON_SHOWN = new EventType<>(Event.ANY, "AUTOSUGGEST_ON_SHOWN");

    public enum AUTOSUGGESTFX_MODE {
        CACHE_DATA,
        LIVE_DATA,
        SEARCH_ENGINE
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
    private SearchTimerTask timerTask = new SearchTimerTask();
    private Timer scheduler = new Timer();

    /**************************************************************************
     * Public Properties
     **************************************************************************/

    private boolean lazyMode = true;
    private boolean acceptFreeTextValue = false;
    private int delay = 300; // delay in ms
    private int visibleRowsCount = 10;
    private boolean editable = true;
    private boolean isFullSearch = false;
    private boolean ignoreCase = false;
    private ObjectProperty<T> item = new SimpleObjectProperty<>(null);
    private ObjectProperty<B> bean = new SimpleObjectProperty<>(null);
    private BooleanProperty loadingIndicator = new SimpleBooleanProperty(false);
    private StringProperty searchStatus = new SimpleStringProperty(String.valueOf(STATUS_SEARCH.NOTHING));
    private BooleanProperty controlShown = new SimpleBooleanProperty(true);

    // TODO these Function should be <T, ...
    private Function<String, List<KeyValueString>> searchFunction = null;
    private Function<String, List<KeyValueString>> dataSource = s -> null;
    private Function<KeyValueString, String> stringTextFormatter = item -> String.format("%s", item.getValue());
    private Function<KeyValueString, String> stringItemFormatter = null;
    private Function<KeyValueString, Node> nodeItemFormatter = null;
    private InvalidationListener beanl = observable -> beanProperty();

    // Observable o (there is the B bean inside) ==> T item converter
    private Function<Observable, T> beanToItemMapping = o -> null;

    // T bean inside ==> B value converter
    private Function<Observable, B> itemToBeanMapping = item -> null;

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
        Version.getInstance();
    }

    /**
     * Creates a new AutosuggestComboBoxList instance
     *
     * @param items The items to display.
     */
    public AutosuggestComboBoxList(final ObservableList<T> items) {
        this.items = items == null ? FXCollections.<T>observableArrayList() : items;

        // apply user mapping
        beanl = new InvalidationListener() {
            @Override
            public void invalidated(Observable bean) {
                LOG.debug(" -------------> BEAN FORMATTER               ");
                T kv = beanToItemMapping.apply(bean);
                KeyValue kvi = new KeyValueStringImpl(String.valueOf(kv.getKey()), String.valueOf(kv.getValue()));
                itemProperty().setValue((T) kvi);
                if (getSkinControl() != null) {
                    getSkinControl().getCombo().valueProperty().setValue((T) kvi);
                    getSkinControl().setUserInput(String.valueOf(kvi.getValue()));
                    getSkinControl().getButton().setText(String.valueOf(kv.getValue()));
                }
                LOG.debug("                BEAN FORMATTER <-------------");
            }
        };
        bean.addListener(beanl);
    }

    public void updateBean(Observable item) {
        bean.removeListener(beanl);
        LOG.debug(" -------------> item FORMATTER               ");
        B theBean = itemToBeanMapping.apply(item);
        beanProperty().setValue(theBean);
        LOG.debug("                item FORMATTER <-------------");
        bean.addListener(beanl);
    }

    /**************************************************************************
     * Helper
     **************************************************************************/
    private void configure(AUTOSUGGESTFX_MODE autosuggestfx_mode) {
        switch (autosuggestfx_mode) {
            case CACHE_DATA:
                isFullSearch = false;
                ignoreCase = false;
                lazyMode = false;
                delay = 100;
                visibleRowsCount = 0;
                acceptFreeTextValue = false;
                break;
            case LIVE_DATA:
                isFullSearch = true;
                ignoreCase = true;
                lazyMode = true;
                delay = 500;
                visibleRowsCount = 10;
                acceptFreeTextValue = false;
                break;
            case SEARCH_ENGINE:
                isFullSearch = true;
                ignoreCase = true;
                lazyMode = true;
                delay = 500;
                visibleRowsCount = 10;
                acceptFreeTextValue = true;
                break;
        }
    }

    public void setCacheDataMode() {
        this.configure(AUTOSUGGESTFX_MODE.CACHE_DATA);
    }

    public void setLiveDataMode() {
        this.configure(AUTOSUGGESTFX_MODE.LIVE_DATA);
    }

    public void setSearchEngineMode() {
        this.configure(AUTOSUGGESTFX_MODE.SEARCH_ENGINE);
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
        AutosuggestComboBoxListSkin<B, T> skin;
        // if an item is loaded, button is shown
        if (itemProperty().getValue() != null) {
            setControlShown(false);
            skin = new AutosuggestComboBoxListSkin<>(this, item);
        } else {
            skin = new AutosuggestComboBoxListSkin<>(this);
        }

        // TODO callback
        endControlInitialization();
        return skin;
    }

    @Override
    public void endControlInitialization() {
        // default search
        searchFunction = term -> getDataSource().stream().filter(item -> {
            if (isIgnoreCase()) {
                return ((isFullSearch ? item.getKey().toLowerCase() : "") + item.getValue().toLowerCase()).contains(term == null ? "" : term.toLowerCase());
            } else {
                return ((isFullSearch ? item.getKey() : "") + item.getValue()).contains(term == null ? "" : term);
            }
        }).collect(Collectors.toList());
    }

    /**************************************************************************
     * Public Properties
     **************************************************************************/

    public AutosuggestComboBoxListSkin<B, T> getSkinControl() {
        return (AutosuggestComboBoxListSkin) getSkin();
    }

    public ObservableList<T> getItems() {
        return items;
    }

    public void setEditorText(String text) {
        getSkinControl().getCombo().getEditor().setText(text);
    }

    public String getEditorText() {
        return getSkinControl().getCombo().getEditor().getText();
    }

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

    public boolean isControlShown() {
        return controlShown.get();
    }

    public BooleanProperty controlShownProperty() {
        return controlShown;
    }

    public void setControlShown(boolean controlShown) {
        this.controlShown.set(controlShown);
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
        return (T) getSkinControl().getCombo().getValue();
    }

    public Function<KeyValueString, String> getStringTextFormatter() {
        return stringTextFormatter;
    }

    public void setStringTextFormatter(Function<KeyValueString, String> stringTextFormatter) {
        this.stringTextFormatter = stringTextFormatter;
    }

    public Function<KeyValueString, String> getStringItemFormatter() {
        return stringItemFormatter;
    }

    public void setStringItemFormatter(Function<KeyValueString, String> stringItemFormatter) {
        this.stringItemFormatter = stringItemFormatter;
    }

    public boolean isFullSearch() {
        return isFullSearch;
    }

    public void setIsFullSearch(boolean isFullSearch) {
        this.isFullSearch = isFullSearch;
    }

    // TODO getSkinControl() could generate null exception , if called before skin is created. Implement a callback to indicate the end of initialisation
    public void setColumnSeparator(String columnSeparator) {
        getSkinControl().setColumnSeparator(columnSeparator);
    }

    public String getColumnSeparator() {
        return getSkinControl().getColumnSeparator();
    }

    public boolean isColumnSeparatorVisible() {
        return getSkinControl().isColumnSeparatorVisible();
    }

    public void setColumnSeparatorVisible(boolean columnSeparatorVisible) {
        getSkinControl().setColumnSeparatorVisible(columnSeparatorVisible);
    }

    public void setKeyValueSeparator(String keyValueSeparator) {
        getSkinControl().setKeyValueSeparator(keyValueSeparator);
    }

    public String getKeyValueSeparator() {
        return getSkinControl().getKeyValueSeparator();
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public Function<KeyValueString, Node> getNodeItemFormatter() {
        return nodeItemFormatter;
    }

    public void setNodeItemFormatter(Function<KeyValueString, Node> nodeItemFormatter) {
        this.nodeItemFormatter = nodeItemFormatter;
    }

    public T getItem() {
        return item.get();
    }

    public ObjectProperty<T> itemProperty() {
        return item;
    }

    public void setItem(T item) {
        this.item.set(item);
    }

    public B getBean() {
        return beanProperty().getValue();
    }

    public ObjectProperty<B> beanProperty() {
        return bean;
    }

    public void setBean(B b) {
        beanProperty().setValue(b);
    }

    public Function<Observable, T> getBeanToItemMapping() {
        return beanToItemMapping;
    }

    public void setBeanToItemMapping(Function<Observable, T> beanToItemMapping) {
        this.beanToItemMapping = beanToItemMapping;
    }

    public Function<Observable, B> getItemToBeamMapping() {
        return itemToBeanMapping;
    }

    public void setItemToBeamMapping(Function<Observable, B> itemToBeamMapping) {
        this.itemToBeanMapping = itemToBeamMapping;
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

    public void setupAndStart(Function<String, List<KeyValueString>> datas, Function<KeyValueString, String> stringTextFormatter, Function<KeyValueString, String> stringItemFormatter) {
        setDataSource(datas);
        setStringTextFormatter(stringTextFormatter);
        setStringItemFormatter(stringItemFormatter);
        start();
    }

    public void start() {
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
                String inputUser = getEditorText();
                list.setAll((Collection<? extends T>) t.getSource().getValue());    // retrieve the T call()
                setEditorText(inputUser);
                stopSearch();
            });
        }

        @Override
        protected T call() throws Exception {
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
        getSkinControl().getCombo().getEditor().positionCaret(getEditorText().length());
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