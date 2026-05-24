package com.bananarepublic;

import com.bananarepublic.ui.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    private static final String APP_TITLE = "Banana Republic";

    @Override
    public void start(Stage stage) {
        Navigator.init(stage);
        stage.setTitle(APP_TITLE);
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        Navigator.goTo("/fxml/main_menu.fxml");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
