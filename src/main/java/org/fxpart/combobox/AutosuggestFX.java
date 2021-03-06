package org.fxpart.combobox;

import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import org.fxpart.common.AbstractAutosuggestControl;
import org.fxpart.common.bean.KeyValue;
import org.fxpart.common.util.AutosuggestFXTimer;
import org.fxpart.common.util.CollectionsUtil;
import org.fxpart.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by metairie on 07-Jul-15.
 * TODO #0 getSkinControl() could generate null exception, if called before skin is created.
 * TODO #0 Replace by a Callback to ensure endControlInitialization() is done
 */
public class AutosuggestFX<B, T extends KeyValue> extends AbstractAutosuggestControl {

    public static final EventType<Event> ON_SHOWN = new EventType<>(Event.ANY, "AUTOSUGGEST_ON_SHOWN");
    private final static Logger LOG = LoggerFactory.getLogger(AutosuggestFX.class);
    // set a new instance of T
    public Function<Observable, T> newInstanceOfT = observable -> null;
    // set a new instance of B
    public Function<Observable, B> newInstanceOfB = observable -> null;
    /**************************************************************************
     * Public Properties
     **************************************************************************/

    // configuration       -----------------------
    private IntegerProperty delay = new SimpleIntegerProperty(this, "delay", 100);                        // delay in ms, max 5000
    private int visibleRowsCount = 10;              // display only 10 items
    private boolean lazyMode = true;                // load before or not datasource
    private boolean acceptFreeTextValue = false;    // ability to input value not in the datasource list
    private boolean isFullSearch = false;           // search on all columns Key + Value(s)
    private int limitSearch = 0;                    // number of input character needed for searching, max 20
    private BooleanProperty ignoreCase = new SimpleBooleanProperty(this, "ignoreCase", false);             // ignore case when searching
    private boolean multiple = false;               // for having multiple choices
    private boolean autoselect = false;             // when the word searched is found, select the first items automatically

    private BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true);                // combo is editable
    private boolean alwaysRefresh = false;          // trigger a search after each input letter
    private boolean refreshFXML = false;            // TODO to be removed
    private BooleanProperty graphicalRendering = new SimpleBooleanProperty(this, "graphicalRendering", true);      // use for combo : Node cell factory (true) or String cell factory

    // properties updated in control or skin       -----------------------
    private BooleanProperty activityIndicator = new SimpleBooleanProperty(this, "activityIndicator", false);
    private StringProperty searchStatus = new SimpleStringProperty(this, "searchStatus", STATUS_SEARCH.NOTHING.name());
    private BooleanProperty controlShown = new SimpleBooleanProperty(this, "controlShown", true);
    private BooleanProperty askReschedule = new SimpleBooleanProperty(this, "askReschedule", false);
    private StringProperty promptText = new SimpleStringProperty(this, "promptText", "Please enter text here");

    // filter a datasource which not supposed to change (array list...)
    private Function<String, List<T>> filter = null;
    // search from a source which is dynamic (database, ws, ...)
    private ObjectProperty<Function<String, List<T>>> search = new SimpleObjectProperty<>(this, "search");
    private ObjectProperty<Supplier<List<T>>> dataSource = new SimpleObjectProperty<>(this, "dataSource", ArrayList::new);

    // formatter        -----------------------
    private ObjectProperty<Function<T, String>> stringTextFormatter = new SimpleObjectProperty<>(this, "stringTextFormatter", item -> String.format("%s", item == null ? "" : item.getValue()));
    private ObjectProperty<Function<T, String>> stringItemFormatter = new SimpleObjectProperty<>(this, "stringItemFormatter", item -> String.format("%s - %s", item == null ? "" : item.getKey(), item == null ? "" : item.getValue()));
    private Function<T, Node> nodeItemFormatter = null;

    // T B manager      -----------------------
    private ObservableList<T> items;
    private ObjectProperty<T> item = new SimpleObjectProperty<>(this, "item");
    private ObjectProperty<B> bean = new SimpleObjectProperty<>(this, "bean");
    private ChangeListener beanListener = (observable, oldValue, newValue) -> beanProperty();
    // T item ==> B bean mapping
    private Function<Observable, B> itemToBeanMapping = o -> {
        ObjectProperty<B> op = (ObjectProperty<B>) o;
        if (op == null || op.getValue() == null) {
            return null;
        } else {
            return newInstanceOfB.apply(o);
        }
    };
    // B bean ==> T item mapping
    private Function<Observable, T> beanToItemMapping = o -> {
        ObjectProperty<T> op = (ObjectProperty<T>) o;
        if (op == null || op.getValue() == null) {
            return null;
        } else {
            return newInstanceOfT.apply(o);
        }
    };
    /**************************************************************************
     * Private Properties
     **************************************************************************/

    // 1 SearchThread       -----------------------
    //private static ExecutorService executorSearch = Executors.newFixedThreadPool(1, new SearchThreadFactory());
    private AutosuggestFXTimer scheduler = null;//AutosuggestFXTimer.getInstance();
    private FilterTimerTask filterTask = null;
    private SearchTimerTask searchTask = null;
    private BooleanProperty logDebug = new SimpleBooleanProperty(this, "debug", false);
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
    private Function<T, String> keyTextFormatter = t -> String.valueOf(t.getKey());

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
        editable.addListener((observable, oldValue, newValue) -> {
            if (oldValue) {
                this.setDisable(true);
            } else {
                this.setHasFocus(false);
                this.setDisable(false);
            }
        });
        this.items = items == null ? FXCollections.<T>observableArrayList() : items;
    }

    public boolean getLogDebug() {
        return logDebug.get();
    }

    public void setLogDebug(boolean logDebug) {
        this.logDebug.set(logDebug);
    }


    /**************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    public BooleanProperty logDebugProperty() {
        return logDebug;
    }

    /**
     * Setup with an external search function
     *
     * @param search
     * @param datasource
     * @param stringTextFormatter
     */
    public void setupSearch(Function<String, List<T>> search, Supplier<List<T>> datasource, Function<T, String> stringTextFormatter) {
        this.search.setValue(search);
        setupFilter(datasource, stringTextFormatter);
    }

    /**************************************************************************
     *
     * Implementation, Public Methods
     *
     **************************************************************************/

    /**
     * Setup with a datasource. Searching is done by control itself
     *
     * @param datas
     * @param stringTextFormatter
     */
    public void setupFilter(Supplier<List<T>> datas, Function<T, String> stringTextFormatter, Function<T, String> stringItemFormatter) {
        this.stringItemFormatter.setValue(stringItemFormatter);
        this.setupFilter(datas, stringTextFormatter);
    }

    /**
     * Setup with a datasource. Searching is done by control itself
     *
     * @param datas
     * @param stringTextFormatter
     */
    public void setupFilter(Supplier<List<T>> datas, Function<T, String> stringTextFormatter) {
        this.dataSource.setValue(datas);
        this.stringTextFormatter.setValue(stringTextFormatter);
        start();
    }

    /**
     * Call a load if necessary
     */
    public void start() {
        if (!lazyMode) {
            if (!(itemProperty().getValue() == null && beanProperty().getValue() == null)) {
                reSchedule(null, alwaysRefresh);
            }
        }
    }

    public void applyList(String term, List<T> newList) {
        if (Objects.equals(term, getSkinControl().getCombo().getEditor().getText()) || term.isEmpty()) {
            applyList(newList);
        }
    }

    /**
     * Method for doing the update of cells
     *
     * @param newList
     */
    public void applyList(List<T> newList) {
        if (newList == null) {
            return;
        }
        Collection<? extends T> cListCopy = CollectionsUtil.split(newList, determineListItemSize(newList));
        // set the combo item list in one time
        String searchTerm = getSkinControl().getCombo().getEditor().getText();

//        getSkinControl().getCombo().setItems(null);

        items.setAll(cListCopy);

//        getSkinControl().getCombo().setItems(FXCollections.observableArrayList(cListCopy));
//        ComboBoxListViewSkin skin = (ComboBoxListViewSkin) getSkinControl().getCombo().getSkin();
//        skin.getListView().refresh();

        // prevent bug when a key type when a select item has set the editor text
        // because the item is refresh when the list is clear or replace by setAll
        if (!getSkinControl().getCombo().getEditor().getText().equals(searchTerm)) {
            getSkinControl().getCombo().getEditor().setText(searchTerm);
            getSkinControl().getCombo().getEditor().selectPositionCaret(searchTerm.length());
            getSkinControl().getCombo().getEditor().deselect();
        }

        getSkinControl().show();
    }

    public void appendResult(String finalTerm, List<T> newList) {
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
     *
     * @param event
     * @param refresh do a search again
     */
    public void reSchedule(Event event, boolean refresh) {
        scheduler = AutosuggestFXTimer.getInstance();
        if (refresh) {
            // ask a new external Search
            getSearch().apply(getSkinControl().getCombo().getEditor().getText());
//            searchTask = new SearchTimerTask(event);
//            scheduler.schedule(searchTask, this.delay.getValue(), this.delay.getValue());
        } else {
            // apply an internal Stream filter
            filterTask = new FilterTimerTask(event);
            scheduler.schedule(filterTask, this.delay.getValue(), this.delay.getValue());
        }
    }

    public void startFiltering() {
        activityIndicatorProperty().setValue(new Boolean(true));
        searchStatus.setValue(String.valueOf(STATUS_SEARCH.RUN));
        stopScheduler();
    }

    // ----------------------------------------------------------------------- Search Task

    public void stopFiltering() {
        stopScheduler();
        searchStatus.setValue(String.valueOf(STATUS_SEARCH.NOTHING));
        activityIndicatorProperty().setValue(new Boolean(false));
    }

    /**************************************************************************
     * Private Methods
     **************************************************************************/

    private void stopScheduler() {
        dispose();
    }

    // ----------------------------------------------------------------------- Filter Task

    /**************************************************************************
     * Helper
     **************************************************************************/
    private void configure(AUTOSUGGESTFX_MODE autosuggestfx_mode) {
        switch (autosuggestfx_mode) {
            case CACHE_DATA:
                isFullSearch = false;
                ignoreCase.setValue(false);
                lazyMode = false;
                visibleRowsCount = -1;
                acceptFreeTextValue = false;
                break;
            case LIVE_DATA:
                isFullSearch = true;
                ignoreCase.setValue(true);
                acceptFreeTextValue = false;
                break;
            case SEARCH_ENGINE:
                isFullSearch = true;
                ignoreCase.setValue(true);
                acceptFreeTextValue = true;
                break;
        }
    }

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

    /**
     * Determine the "best" size according to different parameters
     * <p>
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

    /**
     * When the AutosuggestFx is used in a form with fields.
     * After the binding, it's mandatory to call this method
     * // TODO problem when system with autodetection bean exists. This method marks then B Bean as modified at startup
     *
     * @param b The Observable Property bind with the AutosuggestFx B bean property
     */
    public void refreshBeanCascade(ObjectProperty<B> b) {
        beanProperty().setValue(b.getValue());
        T kv = beanToItemMapping.apply(b);

        item.setValue(kv);

        // notify change combo
        if (getSkinControl() != null) {
            getSkinControl().getCombo().setValue(kv);
        }

        refreshFXML = true;
    }

    /**
     * This method set the AutosuggestFx B Bean Property.
     * After an Item is set in the Skin, for avoiding infinite listening
     *
     * @param t
     */
    void refreshOnlyBean(ObservableValue<T> t) {
        beanProperty().removeListener(beanListener);
        B b = itemToBeanMapping.apply(t);
        beanProperty().setValue(b);
        beanProperty().addListener(beanListener);
    }

    @Override
    public void endControlInitialization() {
        beanListener = (b, o, n) -> {
            T kv = beanToItemMapping.apply(b);
            item.setValue(kv);
            //
            if (n == null && o != null) {
//                clearAll();
//                getSkinControl().showCombo();
            }

        };
        bean.addListener(beanListener);

        // default filter
        filter = term -> {
            List<T> list = dataSource.get().get().stream().filter(t -> {
                String k = String.valueOf(t.getKey());
                String v = String.valueOf(t.getValue());
                if (ignoreCase.getValue()) {
                    return ((isFullSearch ? k.toLowerCase() : "") + v.toLowerCase()).contains(term == null ? "" : term.toLowerCase());
                } else {
                    return ((isFullSearch ? k : "") + v).contains(term == null ? "" : term);
                }
            }).collect(Collectors.toList());
            list.subList(0, determineListItemSize(list));
            return list;
        };

        // if a value is set.
        if (beanProperty().getValue() != null) {
            T kv = beanToItemMapping.apply(beanProperty());
            item.setValue(kv);
        }
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
        if (filterTask != null) {
            filterTask.cancel();
            filterTask = null;
        }
        if (searchTask != null) {
            searchTask.cancel();
            searchTask = null;
        }
        if (scheduler != null) {
            scheduler.cancel();
            scheduler = null;
        }
    }

    /**
     * clear all objects. AutosuggestFx is like a new
     */
    public void clearAll() {
        setBean(null);
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

    public List<T> getDataSource() {
        return dataSource.get().get();
    }

    public void setDataSource(Supplier<List<T>> dataSource) {
        this.dataSource.setValue(dataSource);
    }

    public Function<String, List<T>> getFilter() {
        return filter;
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

    public boolean getLazyMode() {
        return lazyMode;
    }

    public boolean isLazyMode() {
        return lazyMode;
    }

    public void setLazyMode(boolean lazyMode) {
        this.lazyMode = lazyMode;
    }

    public boolean isAcceptFreeTextValue() {
        return acceptFreeTextValue;
    }

    public void setAcceptFreeTextValue(boolean acceptFreeTextValue) {
        this.acceptFreeTextValue = acceptFreeTextValue;
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

    public String getSearchStatus() {
        return searchStatus.get();
    }

    public void setSearchStatus(String searchStatus) {
        this.searchStatus.set(searchStatus);
    }

    public StringProperty searchStatusProperty() {
        return searchStatus;
    }

    public boolean isControlShown() {
        return controlShown.get();
    }

    public void setControlShown(boolean controlShown) {
        this.controlShown.set(controlShown);
    }

    public BooleanProperty controlShownProperty() {
        return controlShown;
    }

    public boolean getActivityIndicator() {
        return activityIndicator.get();
    }

    public void setActivityIndicator(boolean activityIndicator) {
        this.activityIndicator.set(activityIndicator);
    }

    public BooleanProperty activityIndicatorProperty() {
        return activityIndicator;
    }

    public final T getValue() {
        return getSkinControl().getCombo().getValue();
    }

    public Function<T, String> getStringTextFormatter() {
        return stringTextFormatter.getValue();
    }

    public void setStringTextFormatter(Function<T, String> stringTextFormatter) {
        this.stringTextFormatter.setValue(stringTextFormatter);
    }

    public ObjectProperty<Function<T, String>> stringTextFormatterProperty() {
        return stringTextFormatter;
    }

    public Function<T, String> getStringItemFormatter() {
        return stringItemFormatter.getValue();
    }

    public void setStringItemFormatter(Function<T, String> stringItemFormatter) {
        this.stringItemFormatter.setValue(stringItemFormatter);
    }

    public ObjectProperty<Function<T, String>> stringItemFormatterProperty() {
        return stringItemFormatter;
    }

    public boolean isFullSearch() {
        return isFullSearch;
    }

    public void setIsFullSearch(boolean isFullSearch) {
        this.isFullSearch = isFullSearch;
    }

    public String getColumnSeparator() {
        return getSkinControl().getColumnSeparator();
    }

    public void setColumnSeparator(String columnSeparator) {
        getSkinControl().setColumnSeparator(columnSeparator);
    }

    public boolean isColumnSeparatorVisible() {
        return getSkinControl().isColumnSeparatorVisible();
    }

    public void setColumnSeparatorVisible(boolean columnSeparatorVisible) {
        getSkinControl().setColumnSeparatorVisible(columnSeparatorVisible);
    }

    public String getKeyValueSeparator() {
        return getSkinControl().getKeyValueSeparator();
    }

    public void setKeyValueSeparator(String keyValueSeparator) {
        getSkinControl().setKeyValueSeparator(keyValueSeparator);
    }

    public boolean isIgnoreCase() {
        return ignoreCase.getValue();
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase.setValue(ignoreCase);
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

    public void setItem(T item) {
        this.item.set(item);
    }

    public ObjectProperty<T> itemProperty() {
        return item;
    }

    /**
     * Use valueProperty()
     * @return
     */
    @Deprecated
    public ObjectProperty<B> beanProperty() {
        return bean;
    }

    public ObjectProperty<B> valueProperty() {
        return bean;
    }

    public final B getBean() {
        return beanProperty().get();
    }

    public final void setBean(B b) {
        beanProperty().set(b);
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

    public Function<String, List<T>> getSearch() {
        return search.getValue();
    }

    public void setSearch(Function<String, List<T>> search) {
        this.search.setValue(search);
    }

    public boolean isRefreshFXML() {
        return refreshFXML;
    }

    public void setRefreshFXML(boolean refreshFXML) {
        this.refreshFXML = refreshFXML;
    }

    public int getLimitSearch() {
        return limitSearch;
    }

    public void setLimitSearch(int limitSearch) {
        this.limitSearch = Math.max(0, Math.min(20, limitSearch));
    }

    public boolean getAskReschedule() {
        return askReschedule.get();
    }

    public void setAskReschedule(boolean askReschedule) {
        this.askReschedule.set(askReschedule);
    }

    public BooleanProperty askRescheduleProperty() {
        return askReschedule;
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

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public boolean isAutoselect() {
        return autoselect;
    }

    public void setAutoselect(boolean autoselect) {
        this.autoselect = autoselect;
    }

    // ----------------------------------------------------------------------- On Shown
    public final ObjectProperty<EventHandler<Event>> onShownProperty() {
        return onShown;
    }

    public final EventHandler<Event> getOnShown() {
        return onShownProperty().get();
    }

    public final void setOnShown(EventHandler<Event> value) {
        onShownProperty().set(value);
    }

    public Function<T, String> getKeyTextFormatter() {
        return keyTextFormatter;
    }

    public void setKeyTextFormatter(Function<T, String> keyTextFormatter) {
        this.keyTextFormatter = keyTextFormatter;
    }

    @Override
    public void requestFocus() {
        AutosuggestFXSkin skin = (AutosuggestFXSkin) getSkin();
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

    /**
     * inner Class for external search
     */
    public class SearchTimerTask extends TimerTask {
        final private Event event;

        SearchTimerTask() {
            this(null);
        }

        SearchTimerTask(Event event) {
            this.event = event;
        }

        @Override
        public void run() {
            startFiltering();
            final SearchTask task = new SearchTask(this.event);
            final Service searchService = new Service() {
                @Override
                protected Task createTask() {
                    return task;
                }
            };
            searchService.start();
        }
    }

    public class SearchTask extends Task<List<T>> {
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
                stopFiltering();
                searchStatus.setValue(String.valueOf(STATUS_SEARCH.SUCCESS));
            });
            setOnFailed(t -> {
                searchStatus.setValue(String.valueOf(STATUS_SEARCH.FAIL));
                stopFiltering();
                LOG.debug(String.valueOf(getException()));
            });
        }

        @Override
        protected List<T> call() throws Exception {
            return search.getValue().apply(getEditorText());
        }
    }

    /**
     * inner Class for filtering
     */
    public class FilterTimerTask extends TimerTask {
        final private Event event;

        FilterTimerTask() {
            this(null);
        }

        FilterTimerTask(Event event) {
            this.event = event;
        }

        @Override
        public void run() {
            startFiltering();
            // create a service.
            FilterTask task = new FilterTask(this.event);
            final Service searchService = new Service() {
                @Override
                protected Task createTask() {
                    return task;
                }
            };
            searchService.start();
        }

    }

    public class FilterTask extends Task<List<T>> {
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
                applyList((List<T>) t.getSource().getValue());

                // reset editor text
                setEditorText(inputUser);

                stopFiltering();
            });
            setOnFailed(t -> {
                searchStatus.setValue(String.valueOf(STATUS_SEARCH.FAIL));
                stopFiltering();
                LOG.debug(String.valueOf(getException()));
            });
        }

        @Override
        protected List<T> call() throws Exception {
            return filter.apply(getEditorText());
        }
    }


}