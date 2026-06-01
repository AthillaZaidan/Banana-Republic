package com.bananarepublic;

import com.bananarepublic.ui.Navigator;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class App extends Application {
    private static final String APP_TITLE = "Banana Republic";

    @Override
    public void start(Stage stage) {
        loadFonts();
        Navigator.init(stage);
        stage.setTitle(APP_TITLE);
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        Navigator.goTo("/fxml/main_menu.fxml");
        stage.show();
    }

    private void loadFonts() {
        String[] weights = {"ExtraLight", "Light", "Regular", "Medium", "SemiBold", "Bold", "ExtraBold"};
        for (String w : weights) {
            Font.loadFont(App.class.getResourceAsStream(
                    "/fonts/GemunuLibre/GemunuLibre-" + w + ".ttf"), 14);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
