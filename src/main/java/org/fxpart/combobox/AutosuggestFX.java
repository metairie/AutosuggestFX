package org.fxpart.combobox;

import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import org.fxpart.common.AbstractAutosuggestControl;
import org.fxpart.common.bean.KeyValue;
import org.fxpart.common.util.CollectionsUtil;
import org.fxpart.common.util.SearchThreadFactory;
import org.fxpart.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by metairie on 07-Jul-15.
 * TODO #0 getSkinControl() could generate null exception, if called before skin is created.
 * TODO #0 Replace by a Callback to ensure endControlInitialization() is done
 */
public class AutosuggestFX<B, T extends KeyValue> extends AbstractAutosuggestControl {

    private final static Logger LOG = LoggerFactory.getLogger(AutosuggestFX.class);
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

    // 1 SearchThread for filtering + 1 SearchThread for searching
    private ExecutorService executor = Executors.newFixedThreadPool(2, new SearchThreadFactory());
    private ObservableList<T> items;
    private FilterTimerTask timerTask = null;
    private SearchTimerTask searchTask = null;

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
    private boolean alwaysRefresh = false;

    private ObjectProperty<T> item = new SimpleObjectProperty<>(null);
    private ObjectProperty<B> bean = new SimpleObjectProperty<>(this, "bean");

    private BooleanProperty filteringIndicator = new SimpleBooleanProperty(new Boolean(false));
    private BooleanProperty searchingIndicator = new SimpleBooleanProperty(new Boolean(false));

    private StringProperty searchStatus = new SimpleStringProperty(String.valueOf(STATUS_SEARCH.NOTHING));
    private BooleanProperty controlShown = new SimpleBooleanProperty(new Boolean(true));
    private Function<String, List<T>> filter = null;
    private Function<String, List<T>> search = null;
    private Function<String, List<T>> dataSource = s -> new ArrayList<>();
    private Function<T, String> stringTextFormatter = item -> String.format("%s", item.getValue());
    private Function<T, String> stringItemFormatter = item -> String.format("%s - %s", item.getKey(), item.getValue());
    private Function<T, Node> nodeItemFormatter = null;

    //    private InvalidationListener beanListener = observable -> beanProperty();
    private ChangeListener beanListener = new ChangeListener() {
        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue) {
            beanProperty();
        }
    };

    // mapping between B and T
    // set a new instance of T or B
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
    public AutosuggestFX() {
        this(null);
        Version.getInstance();
    }

    /**
     * Creates a new AutosuggestComboBoxList instance
     *
     * @param items The items to display.
     */
    public AutosuggestFX(final ObservableList<T> items) {
        this.items = items == null ? FXCollections.<T>observableArrayList() : items;
    }

    public void setupAndStart(Function<String, List<T>> search, Function<String, List<T>> datasource, Function<T, String> stringTextFormatter, Function<T, String> stringItemFormatter) {
        this.search = search;
        this.setupAndStart(datasource, stringTextFormatter, stringItemFormatter);
    }

    public void setupAndStart(Function<String, List<T>> datas, Function<T, String> stringTextFormatter, Function<T, String> stringItemFormatter) {
        this.dataSource = datas;
        this.stringTextFormatter = stringTextFormatter;
        this.stringItemFormatter = stringItemFormatter;
        start();
    }

    public void start() {
        if (!lazyMode) {
            if (!(itemProperty().getValue() == null && beanProperty().getValue() == null)) {
                reSchedule(null, alwaysRefresh);
            }
        }
    }

    /**************************************************************************
     * Implementation, Public Methods
     **************************************************************************/

    public void reSchedule(Event event, boolean doSearch) {
        if (scheduler != null) {
            scheduler.purge();
            scheduler.cancel();
        }
        scheduler = new Timer();

        // running timer task as daemon thread
        if (doSearch) {
            searchTask = new SearchTimerTask(event);
            scheduler.schedule(searchTask, this.delay, this.delay);

        } else {
            timerTask = new FilterTimerTask(event);
            scheduler.schedule(timerTask, this.delay, this.delay);
        }
    }

    /**
     * Class for external search
     */
    public class SearchTimerTask extends TimerTask {
        SearchTimerTask() {
            this(null);
        }

        private Event event;

        SearchTimerTask(Event event) {
            filteringIndicatorProperty().setValue(new Boolean(true));
            this.event = event;
        }

        @Override
        public void run() {
            startFiltering();
            SearchTask<T> searchTask = new SearchTask<>(this.event);
            executor.submit(searchTask);
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
                stopFiltering();
                LOG.debug(String.valueOf(getException()));
            });
            setOnSucceeded(t -> {
                searchStatus.setValue(String.valueOf(STATUS_SEARCH.SUCCESS));
                stopFiltering();
            });
        }

        @Override
        protected T call() throws Exception {
            return (T) search.apply(getEditorText());
        }
    }

    /**
     * Method for trigger the update of cells
     *
     * @param newList
     */
    public void applyList(Object newList) {
        // list items of Combo
        ObservableList<T> list = (ObservableList<T>) items;

        // T call() . List comes from a datasource
        Collection<? extends T> cList = (Collection<? extends T>) newList;
        Collection<? extends T> cListCopy = CollectionsUtil.split(cList, determineListItemSize((List) cList));

        // set the combo item list in one time
        list.setAll(cListCopy);
    }

    /**
     * Class for filtering
     */
    public class FilterTimerTask extends TimerTask {
        FilterTimerTask() {
            this(null);
        }

        FilterTimerTask(Event event) {
            filteringIndicatorProperty().setValue(new Boolean(true));
            this.event = event;
        }

        private Event event;

        @Override
        public void run() {
            startFiltering();
            FilterTask<T> searchTask = new FilterTask<>(this.event);
            executor.submit(searchTask);
        }
    }

    public class FilterTask<T> extends Task<T> {
        private Event event;

        FilterTask() {
            this(null);
        }

        FilterTask(Event event) {
            this.event = event;
            setOnCancelled(t -> {
                searchStatus.setValue(String.valueOf(STATUS_SEARCH.FAIL));
                stopFiltering();
                LOG.debug(String.valueOf(getException()));
            });
            setOnSucceeded(t -> {
                searchStatus.setValue(String.valueOf(STATUS_SEARCH.SUCCESS));
                String inputUser = getEditorText();

                // apply new list
                applyList(t.getSource().getValue());

                // reset editor text
                setEditorText(inputUser);
                stopFiltering();
            });
        }

        @Override
        protected T call() throws Exception {
            return (T) filter.apply(getEditorText());
        }
    }

    public void startFiltering() {
        searchStatus.setValue(String.valueOf(STATUS_SEARCH.RUN));
        stopScheduler();
    }

    public void stopFiltering() {
        stopScheduler();
        filteringIndicatorProperty().setValue(new Boolean(false));
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
     * <p/>
     * If visibleRowsCount <= -1 display ALL list
     *
     * @param list
     * @return
     */
    public int determineListItemSize(List list) {
        if (list != null) {
            return (visibleRowsCount <= -1 ? list.size() : visibleRowsCount < list.size() ? visibleRowsCount : list.size());
        } else {
            return visibleRowsCount;
        }
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
        AutosuggestFXSkin<B, T> skin;
        // if an item is loaded, button is shown
        if (itemProperty().getValue() != null) {
            setControlShown(new Boolean(false));
            skin = new AutosuggestFXSkin<>(this, item);
        } else {
            skin = new AutosuggestFXSkin<>(this);
        }

        endControlInitialization();
        return skin;
    }

    public void refresh(ObservableValue t) {
        beanProperty().removeListener(beanListener);
        beanProperty().setValue(itemToBeanMapping.apply(t));
        beanProperty().addListener(beanListener);
    }

    @Override
    public void endControlInitialization() {
        beanListener = new ChangeListener() {
            @Override
            public void changed(ObservableValue b, Object o, Object n) {
                T kv = beanToItemMapping.apply(b);
                item.setValue(kv);
            }
        };
        bean.addListener(beanListener);

        // default filter
        filter = term -> {
            List<T> list = dataSource.apply(null).stream().filter(t -> {
                String k = String.valueOf(t.getKey());
                String v = String.valueOf(t.getValue());
                if (ignoreCase) {
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

    public AutosuggestFXSkin<B, T> getSkinControl() {
        return (AutosuggestFXSkin) getSkin();
    }

    public ObservableList<T> getItems() {
        return items;
    }

    public void setItems(ObservableList<T> items) {
        this.items = items;
    }

    public void setEditorText(String text) {
        getSkinControl().getCombo().getEditor().setText(text);
    }

    public String getEditorText() {
        if (getSkinControl() == null) {
            return "";
        } else
            return getSkinControl().getCombo().getEditor().getText();
    }

    public List<T> getDataSource() {
        return dataSource.apply(null);
    }

    public void setDataSource(Function<String, List<T>> dataSource) {
        this.dataSource = dataSource;
    }

    public Function<String, List<T>> getFilter() {
        return filter;
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

    public boolean getFilteringIndicator() {
        return filteringIndicator.get();
    }

    public BooleanProperty filteringIndicatorProperty() {
        return filteringIndicator;
    }

    public void setFilteringIndicator(boolean filteringIndicator) {
        this.filteringIndicator.set(filteringIndicator);
    }

    public boolean getSearchingIndicator() {
        return searchingIndicator.get();
    }

    public BooleanProperty searchingIndicatorProperty() {
        return searchingIndicator;
    }

    public void setSearchingIndicator(boolean searchingIndicator) {
        this.searchingIndicator.set(searchingIndicator);
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

    public boolean isAlwaysRefresh() {
        return alwaysRefresh;
    }

    public void setAlwaysRefresh(boolean alwaysRefresh) {
        this.alwaysRefresh = alwaysRefresh;
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

    public ObjectProperty<B> beanProperty() {
        return bean;
    }

    public final void setBean(B b) {
        beanProperty().set(b);
    }

    public final B getBean() {
        return beanProperty().get();
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

    public Function getSearch() {
        return search;
    }

    public void setSearch(Function search) {
        this.search = search;
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
     * Called just after the {@link AbstractAutosuggestControl} popup/display is shown.
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