package org.fxpart.combobox;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by metairie on 07-Jul-15.
 */
public class AutosuggestComboBoxList<T> extends AutosuggestControl {
    /**************************************************************************
     * Private fields
     **************************************************************************/

    private final ObservableList<T> items;
    private boolean waitFlag = false;

    // TODO remove this by skinProperty ?
    private ComboBox<T> combo;

    private String searchString = "";
    private int timer = 500;
    private Function<String, List<KeyValueString>> searchFunction;
    private Function<String, List<KeyValueString>> dataSource;
    private Function<KeyValueString, String> textFieldFormatter; //= item -> String.format("%s", item.getValue());
    private Function<KeyValueString, String> labelItemFormatter; // = item -> String.format("%s - %s", item.getKey(), item.getValue());


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
        return new AutosuggestComboBoxListSkin<>(this);
    }

    /**
     * Represents the list of items.
     */
    public ObservableList<T> getItems() {
        return items;
    }

    public ComboBox<T> getCombo() {
        return combo;
    }

    public void setCombo(ComboBox<T> combo) {
        this.combo = combo;
    }

    public boolean getWaitFlag() {
        return waitFlag;
    }

    public void setWaitFlag(boolean waitFlag) {
        this.waitFlag = waitFlag;
    }

    // TODO remove this
    public TextField getEditor() {
        return getCombo().getEditor();
    }

    public T getValue() {
        return getCombo().getValue();
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

    public int getTimer() {
        return this.timer;
    }

    public void setTimer(int timer) {
        this.timer = timer;
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


    /**************************************************************************
     * Properties
     **************************************************************************/

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
        this.searchFunction = (term -> getDataSource().stream().filter(item -> item.getValue().contains(term == null ? "" : term)).collect(Collectors.toList()));
        this.textFieldFormatter = kvs -> String.format("%s", kvs.getValue());
        ObservableList<T> list = FXCollections.observableArrayList((Collection<? extends T>) getSearchFunction().apply(null));
        setWaitFlag(true);
    }


    /**************************************************************************
     * Implementation
     **************************************************************************/

    public void doSearch(Event event) {
        DelayedSearchTask delayedSearchTask = new DelayedSearchTask(this, getTimer(), event);
        Thread delayedSearchThread = new Thread(delayedSearchTask);
        delayedSearchThread.start();
    }
}