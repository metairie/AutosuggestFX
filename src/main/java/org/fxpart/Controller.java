package org.fxpart;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.fxpart.combobox.AutosuggestComboBoxList;
import org.fxpart.combobox.DeprecatedAutosuggestComboBoxList;
import org.fxpart.combobox.KeyValueString;
import org.fxpart.mockserver.MockDatas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

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
//        autosuggest.setTimer(1000);
        autosuggest.init(o -> new MockDatas().loadLocation(), item -> String.format("%s", item.getValue()));
    }

//    // framework.search function for combo
//    private Function<String, List<KeyValueString>> searchFunctionParam(List<KeyValueString> items) {
//        return term -> items.stream().filter(item -> item.getValue().contains(term == null ? "" : term)).collect(Collectors.toList());
//    }
//
//    // text fields formatter
//    private Function<KeyValueString, String> textFieldFormatter = item -> String.format("%s", item.getValue());
//
//    // label formatter
//    private Function<KeyValueString, String> labelItemFormatter = item -> String.format("%s - %s", item.getKey(), item.getValue());

    public void clickOnShow(Event event) {
//        autosuggestComboBoxList.doSearch(event);
    }
}