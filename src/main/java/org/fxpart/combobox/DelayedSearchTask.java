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

    AutosuggestComboBoxList control;
    Integer delay;
    Event event;

    public DelayedSearchTask(AutosuggestComboBoxList autosuggest, Integer delay, Event event) {
        this.control = autosuggest;
        this.delay = delay;
        this.event = event;
    }

    @Override
    public void run() {
        if (control.getWaitFlag()) {
            synchronized (this) {
                try {
                    control.setLoadingIndicator(true);
                    wait(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                control.setWaitFlag(false);
            }
        }

        Platform.runLater(new Runnable() {
            public void run() {
                String searchString = control.getEditor().getText();
                ObservableList list = control.getItems();
                list.clear();
                System.out.println("Id " + this.hashCode() + " = " + searchString + " ------------------------ there is a Hit against the server there ------------- ");

                list.setAll((Collection<? extends KeyValueString>) control.getSearchFunction().apply(searchString));
                if (control.getValue() == null) {
                    control.getEditor().setText(searchString);
                }
                control.setSearchString(searchString);
                control.getEditor().positionCaret(searchString.length());
                // TODO remove this
                if (KeyEvent.KEY_RELEASED == event.getEventType() && !list.isEmpty()) {
                    control.getCombo().show();
                }
                control.setLoadingIndicator(false);
            }
        });
    }
}
