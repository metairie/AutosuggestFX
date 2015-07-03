package org.fxpart;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.mockserver.integration.ClientAndServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private final static Logger LOG = LoggerFactory.getLogger(Main.class);
    private ClientAndServer mockServer;

    @Override
    public void start(Stage primaryStage) throws Exception {
        LOG.info("Fx loading");
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Hello World");

        Scene scene = new Scene(root, 1024, 768);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
