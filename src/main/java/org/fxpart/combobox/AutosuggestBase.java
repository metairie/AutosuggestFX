package org.fxpart.combobox;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.ComboBox;

import java.util.List;
import java.util.function.Function;

/**
 * Created by metairie on 06-Jul-15.
 */
public abstract class AutosuggestBase<T> extends ComboBox<T> {
    private DoubleProperty fixedHeight = new SimpleDoubleProperty(150);
    private int visibleRowsCount = 10;
    private String searchString = "";
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
     *
     * @param timer
     */
    private Integer timer = 1000;
    private Boolean waitFlag;

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public Function<String, List<KeyValueString>> getSearchFunction() {
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

    public void setLazyMode(boolean lazyMode) {
        this.lazyMode = lazyMode;
    }

    public boolean getLazyMode() {
        return lazyMode;
    }

    public boolean setLoadingIndicator() {
        return loadingIndicator;
    }

    public void setLoadingIndicator(boolean loadingIndicator) {
        this.loadingIndicator = loadingIndicator;
    }

    public AutosuggestBase() {
        setEditable(true);
    }

    public void setDataSource(Function<String, List<KeyValueString>> dataSource) {
        this.dataSource = dataSource;
    }

    public List<KeyValueString> getDataSource() {
        return dataSource.apply(null);
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchFunction(Function<String, List<KeyValueString>> searchFunction) {
        this.searchFunction = searchFunction;
    }

    public boolean isLazyMode() {
        return lazyMode;
    }

    public boolean isLoadingIndicator() {
        return loadingIndicator;
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
}
