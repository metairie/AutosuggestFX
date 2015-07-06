package org.fxpart.combobox;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.util.Collection;

/**
 * Created by Pavlo on 05.07.2015.
 */
public class DelayedSearchTask implements Runnable {
    AutosuggestBase searchEventSource;
    Integer delay;

    public DelayedSearchTask(AutosuggestBase searchEventSource, Integer delay) {
        this.searchEventSource = searchEventSource;
        this.delay = delay;
    }

    @Override
    public void run() {
        if (searchEventSource.getWaitFlag()) {
            synchronized (this) {
                try {
                    wait(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                searchEventSource.setWaitFlag(false);
            }
        }

        Platform.runLater(new Runnable() {
            public void run() {
                String searchString = searchEventSource.getEditor().getText();
                ObservableList list = searchEventSource.getItems();
                list.clear();
                list.setAll((Collection<? extends KeyValueString>) searchEventSource.getSearchFunction().apply(searchString));
                if (searchEventSource.getValue() == null) {
                    searchEventSource.getEditor().setText(searchString);
                }
                searchEventSource.setSearchString(searchString);
                searchEventSource.getEditor().positionCaret(searchString.length());
                if (!list.isEmpty()) {
                    searchEventSource.show();
                }
            }
        });
    }
}
