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
import org.fxpart.CollectionsUtil;
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
    private SearchTimerTask timerTask = null;
    private Timer scheduler = new Timer();

    /**************************************************************************
     * Public Properties
     **************************************************************************/

    private int delay = 300; // delay in ms
    private int visibleRowsCount = 10;

    private boolean lazyMode = true;
    private boolean acceptFreeTextValue = false;
    private boolean editable = true;
    private boolean isFullSearch = false;
    private boolean ignoreCase = false;

    private ObjectProperty<T> item = new SimpleObjectProperty<>(null);
    private ObjectProperty<B> bean = new SimpleObjectProperty<>(null);
    private BooleanProperty loadingIndicator = new SimpleBooleanProperty(new Boolean(false));
    private StringProperty searchStatus = new SimpleStringProperty(String.valueOf(STATUS_SEARCH.NOTHING));
    private BooleanProperty controlShown = new SimpleBooleanProperty(new Boolean(true));
    private Function<String, List<T>> searchFunction = null;
    private Function<String, List<T>> dataSource = s -> null;
    private Function<T, String> stringTextFormatter = item -> String.format("%s", item.getValue());
    private Function<T, String> stringItemFormatter = null;
    private Function<T, Node> nodeItemFormatter = null;
    private InvalidationListener beanListener = observable -> beanProperty();
    private InvalidationListener itemListener = observable -> beanProperty();

    // TODO #3 set a new instance of T or B
//    public Function<Observable, T> newInstanceOfT = t -> (T) new KeyValueStringImpl("", "");
    public Function<Observable, T> newInstanceOfT = t -> null;
    public Function<Observable, B> newInstanceOfB = t -> null;

    // B bean ==> T item mapping
    private Function<Observable, T> beanToItemMapping = o -> null;

    // T item ==> B bean mapping
    private Function<Observable, B> itemToBeanMapping = o -> null;

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
    }

    public void setupAndStart(Function<String, List<T>> datas, Function<T, String> stringTextFormatter, Function<T, String> stringItemFormatter) {
        setDataSource(datas);
        setStringTextFormatter(stringTextFormatter);
        setStringItemFormatter(stringItemFormatter);
        start();
    }

    public void start() {
        if (!lazyMode) {
            if (!(itemProperty().getValue() == null && beanProperty().getValue() == null)) {
                reSchedule(null);
            }
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
            loadingIndicatorProperty().setValue(new Boolean(true));
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
                // list items of Combo
                ObservableList<T> list = (ObservableList<T>) getItems();
                String inputUser = getEditorText();

                // list coming from a datasource
                Collection<? extends T> cList = (Collection<? extends T>) t.getSource().getValue();
                Collection<? extends T> cListCopy = CollectionsUtil.split(cList, determineListItemSize((List) cList));

                // set the combo item list in one time
                list.setAll(cListCopy);

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
        searchStatus.setValue(String.valueOf(STATUS_SEARCH.RUN));
        stopScheduler();
    }

    public void stopSearch() {
        stopScheduler();
        loadingIndicatorProperty().setValue(new Boolean(false));
    }

    public void updateBean(Observable item) {
        bean.removeListener(beanListener);
        beanProperty().setValue(itemToBeanMapping.apply(item));
        bean.addListener(beanListener);

        // TODO JIRA-AUTOSFX-25 Refactoring of updatebean method
        //if (getSkinControl() != null) {
        getSkinControl().refreshSkin(item);
        // }

    }

    /**************************************************************************
     * Private Methods
     **************************************************************************/

    private void stopScheduler() {
        scheduler.purge();
        scheduler.cancel();
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
                visibleRowsCount = -1;
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

    /**
     * Determine the "best" size according to different parameters
     * <p>
     * If visibleRowsCount <= -1 display ALL list
     *
     * @param list
     * @return
     */
    private int determineListItemSize(List list) {
        return (visibleRowsCount <= -1 ? list.size() : visibleRowsCount < list.size() ? visibleRowsCount : list.size());
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
            setControlShown(new Boolean(false));
            skin = new AutosuggestComboBoxListSkin<>(this, item);
        } else {
            skin = new AutosuggestComboBoxListSkin<>(this);
        }

        // TODO #1 callback
        endControlInitialization();
        return skin;
    }

    @Override
    public void endControlInitialization() {
        // apply user mapping
        beanListener = b -> {
            T kv = beanToItemMapping.apply(b);
            itemProperty().setValue(kv);
            if (getSkinControl() != null) {
                getSkinControl().getCombo().valueProperty().setValue(kv);
                getSkinControl().setUserInput(String.valueOf(kv.getValue()));
                getSkinControl().getButton().setText(String.valueOf(kv.getValue()));
            }
        };
        bean.addListener(beanListener);

        // item listener
        // TODO #2 isSelectedItem
        itemListener = t -> {
            getSkinControl().setIsSelectedItem(((ObjectProperty<T>) t).getValue() != null);
        };
        item.addListener(itemListener);

        // default search
        searchFunction = term -> {
            List<T> list = getDataSource().stream().filter(t -> {
                String k = String.valueOf(t.getKey());
                String v = String.valueOf(t.getValue());
                if (AutosuggestComboBoxList.this.isIgnoreCase()) {
                    return ((isFullSearch ? k.toLowerCase() : "") + v.toLowerCase()).contains(term == null ? "" : term.toLowerCase());
                } else {
                    return ((isFullSearch ? k : "") + v).contains(term == null ? "" : term);
                }
            }).collect(Collectors.toList());
            list.subList(0, determineListItemSize(list));
            return list;
        };
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

    public List<T> getDataSource() {
        return dataSource.apply(null);
    }

    public void setDataSource(Function<String, List<T>> dataSource) {
        this.dataSource = dataSource;
    }

    public Function<String, List<T>> getSearchFunction() {
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

    public Function<T, String> getStringTextFormatter() {
        return stringTextFormatter;
    }

    public void setStringTextFormatter(Function<T, String> stringTextFormatter) {
        this.stringTextFormatter = stringTextFormatter;
    }

    public Function<T, String> getStringItemFormatter() {
        return stringItemFormatter;
    }

    public void setStringItemFormatter(Function<T, String> stringItemFormatter) {
        this.stringItemFormatter = stringItemFormatter;
    }

    public boolean isFullSearch() {
        return isFullSearch;
    }

    public void setIsFullSearch(boolean isFullSearch) {
        this.isFullSearch = isFullSearch;
    }

    // TODO #0 getSkinControl() could generate null exception , if called before skin is created. Implement a callback to indicate the end of initialisation
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

    public Function<T, Node> getNodeItemFormatter() {
        return nodeItemFormatter;
    }

    public void setNodeItemFormatter(Function<T, Node> nodeItemFormatter) {
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

    public int getVisibleRowsCount() {
        return visibleRowsCount;
    }

    public void setVisibleRowsCount(int visibleRowsCount) {
        this.visibleRowsCount = visibleRowsCount;
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

}