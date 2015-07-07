package org.fxpart.combobox;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Created by Pavlo on 05.07.2015.
 */
public class DelayedSearchTask implements Runnable {
    private final static Logger LOG = LoggerFactory.getLogger(DelayedSearchTask.class);

    AutosuggestComboBoxList autosuggest;
    Integer delay;
    Event event;

    public DelayedSearchTask(AutosuggestComboBoxList autosuggest, Integer delay, Event event) {
        this.autosuggest = autosuggest;
        this.delay = delay;
        this.event = event;
    }

    @Override
    public void run() {
        if (autosuggest.getWaitFlag()) {
            synchronized (this) {
                try {
                    wait(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                autosuggest.setWaitFlag(false);
            }
        }

        Platform.runLater(new Runnable() {
            public void run() {
                String searchString = autosuggest.getEditor().getText();
                ObservableList list = autosuggest.getItems();
                list.clear();
                list.setAll((Collection<? extends KeyValueString>) autosuggest.getSearchFunction().apply(searchString));
                if (autosuggest.getValue() == null) {
                    autosuggest.getEditor().setText(searchString);
                }
                autosuggest.setSearchString(searchString);
                autosuggest.getEditor().positionCaret(searchString.length());
                // TODO remove this
                if (KeyEvent.KEY_RELEASED == event.getEventType() && !list.isEmpty()) {
                    autosuggest.getCombo().show();
                }
            }
        });
    }
}
