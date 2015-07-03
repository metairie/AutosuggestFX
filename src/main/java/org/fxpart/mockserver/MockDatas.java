package org.fxpart.mockserver;

import javafx.collections.FXCollections;
import org.fxpart.combobox.KeyValueString;
import org.fxpart.combobox.KeyValueStringImpl;
import org.fxpart.combobox.KeyValueStringLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by metairie on 24-Jun-15.
 */
public class MockDatas {
    private final static Logger LOG = LoggerFactory.getLogger(MockDatas.class);


    // --------------------------------------------
    //  static
    // --------------------------------------------

    // ----------
    // LOCATION
    // ----------
    public static String loadLocationJson() {
        // data for Location
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append("{ \"id\": 1, \"code\":\"LO1\", \"name\": \"Point of View\" },");
        sb.append("{ \"id\": 2, \"code\":\"LO2\", \"name\": \"Poland\" },");
        sb.append("{ \"id\": 3, \"code\":\"LO3\", \"name\": \"Forest\" },");
        sb.append("{ \"id\": 4, \"code\":\"LO4\", \"name\": \"Office\" },");
        sb.append("{ \"id\": 5, \"code\":\"LO5\", \"name\": \"Swimming pool\" },");
        sb.append("{ \"id\": 6, \"code\":\"LO6\", \"name\": \"Tribune\" },");
        sb.append("{ \"id\": 7, \"code\":\"LO7\", \"name\": \"Office\" },");
        sb.append("{ \"id\": 8, \"code\":\"LO8\", \"name\": \"Garden\" }");
        sb.append("]");
        return sb.toString();
    }

    public List<KeyValueString> loadLocation() {
        // data for Location
        KeyValueString lb1 = new KeyValueStringImpl("LO1", "Point of View");
        KeyValueString lb2 = new KeyValueStringImpl("LO2", "Poland");
        KeyValueString lb3 = new KeyValueStringImpl("LO3", "Forest");
        KeyValueString lb4 = new KeyValueStringImpl("LO4", "Office");
        KeyValueString lb5 = new KeyValueStringImpl("LO5", "Swimming pool");
        KeyValueString lb6 = new KeyValueStringImpl("LO6", "Tribune");
        KeyValueString lb7 = new KeyValueStringImpl("LO7", "Office");
        KeyValueString lb8 = new KeyValueStringImpl("LO8", "Garden");
        return Arrays.asList(lb1, lb2, lb3, lb4, lb5, lb6, lb7, lb8);
    }

    public static List<KeyValueString> loadStaticLocation() {
        // data for Location
        KeyValueString lb1 = new KeyValueStringImpl("LO1", "Point of View");
        KeyValueString lb2 = new KeyValueStringImpl("LO2", "Poland");
        KeyValueString lb3 = new KeyValueStringImpl("LO3", "Forest");
        KeyValueString lb4 = new KeyValueStringImpl("LO4", "Office");
        KeyValueString lb5 = new KeyValueStringImpl("LO5", "Swimming pool");
        KeyValueString lb6 = new KeyValueStringImpl("LO6", "Tribune");
        KeyValueString lb7 = new KeyValueStringImpl("LO7", "Office");
        KeyValueString lb8 = new KeyValueStringImpl("LO8", "Garden");
        return Arrays.asList(lb1, lb2, lb3, lb4, lb5, lb6, lb7, lb8);
    }

    public static Collection<?> loadLocationStrings() {
        return FXCollections.observableArrayList("Point of View", "Poland", "Forest", "Office", "Swimming pool", "Tribune", "Office", "Garden");
    }

    // ----------
    // PROFESSION
    // ----------
    public static List<KeyValueStringLabel> loadProfession() {
        // data for Profession
        KeyValueString pb1 = new KeyValueStringImpl("PR1", "Photographer");
        KeyValueString pb2 = new KeyValueStringImpl("PR2", "Politic");
        KeyValueString pb3 = new KeyValueStringImpl("PR3", "Poet");
        KeyValueString pb4 = new KeyValueStringImpl("PR4", "Podiatrist");
        KeyValueString pb5 = new KeyValueStringImpl("PR5", "Swimmer");
        KeyValueString pb6 = new KeyValueStringImpl("PR6", "Spokesman");
        KeyValueString pb7 = new KeyValueStringImpl("PR7", "Developer");
        KeyValueString pb8 = new KeyValueStringImpl("PR8", "Gardener");
        return Arrays.asList(pb1, pb2, pb3, pb4, pb5, pb6, pb7, pb8);
    }

    public static String loadProfessionJson() {
        // data for Profession
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append("{ \"id\": 1, \"code\":\"PR1\", \"name\": \"Photographer\" },");
        sb.append("{ \"id\": 2, \"code\":\"PR2\", \"name\": \"Politic\" },");
        sb.append("{ \"id\": 3, \"code\":\"PR3\", \"name\": \"Poet\" },");
        sb.append("{ \"id\": 4, \"code\":\"PR4\", \"name\": \"Podiatrist\" },");
        sb.append("{ \"id\": 5, \"code\":\"PR5\", \"name\": \"Swimmer\" },");
        sb.append("{ \"id\": 6, \"code\":\"PR6\", \"name\": \"Spokesman\" },");
        sb.append("{ \"id\": 7, \"code\":\"PR7\", \"name\": \"Developer\" },");
        sb.append("{ \"id\": 8, \"code\":\"PR8\", \"name\": \"Gardener\" }");
        sb.append("]");
        return sb.toString();
    }
}
