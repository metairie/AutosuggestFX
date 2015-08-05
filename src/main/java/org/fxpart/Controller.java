package org.fxpart;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import org.fxpart.combobox.AutosuggestComboBoxList;
import org.fxpart.combobox.KeyValueString;
import org.fxpart.mockserver.MockDatas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private final static Logger LOG = LoggerFactory.getLogger(Controller.class);

    @FXML
    AutosuggestComboBoxList<KeyValueString> autosuggest;

    @FXML
    ComboBox cb;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        autosuggest.setEditable(true);
        autosuggest.setAcceptFreeTextValue(true);
        autosuggest.setLazyMode(false); // if no item is selected, lazy is useless
        autosuggest.setDelay(300);
        autosuggest.init(o -> new MockDatas().loadLocation(), item -> String.format("%s", item.getValue()));
    }

    public void clickOnShow(Event event) {
        //TODO must shown immediately
    }

    public void click(ActionEvent actionEvent) {
        autosuggest.reSchedule(actionEvent);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        autosuggest.stopSearch();
    }
}