package org.fxpart;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.fxpart.combobox.AutosuggestComboBoxList;
import org.fxpart.combobox.KeyValueString;
import org.fxpart.mockserver.MockDatas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private final static Logger LOG = LoggerFactory.getLogger(Controller.class);

    @FXML
    AutosuggestComboBoxList<KeyValueString> autosuggest;

    private ObjectProperty<KeyValueString> partDecoDataLocationProperty = new SimpleObjectProperty<>();
    private final ObservableList strings = FXCollections.observableArrayList(MockDatas.loadLocationStrings());

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // datas
        final List<KeyValueString> itemsLocation = new org.fxpart.mockserver.MockDatas().loadLocation();

        //pavel
//        autosuggest.setLazyMode(true);
//        autosuggest.setTimer(2000);
        autosuggest.init(o -> new MockDatas().loadLocation(), item -> String.format("%s", item.getValue()));
    }

    public void clickOnShow(Event event) {
        //autosuggest.doSearch(event);
    }

    public void click(ActionEvent actionEvent) {
        autosuggest.reSchedule();
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        autosuggest.stopScheduler();
    }
}