package com.bananarepublic;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    private static final String MAIN_FXML = "/fxml/main.fxml";
    private static final String APP_TITLE = "Banana Republic";

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource(MAIN_FXML));
        Scene scene = new Scene(loader.load());

        stage.setTitle(APP_TITLE);
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
