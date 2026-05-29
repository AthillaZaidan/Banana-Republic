package com.bananarepublic;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FxmlLoadTest extends ApplicationTest {

    @Override
    public void start(javafx.stage.Stage stage) {}

    @Test
    void allFxmlFilesLoadWithoutError() throws Exception {
        String[] fxmls = {
            "/fxml/main_menu.fxml",
            "/fxml/lobby.fxml",
            "/fxml/game.fxml",
            "/fxml/turn_transition.fxml",
            "/fxml/game_result.fxml",
            "/fxml/trade_dialog.fxml",
            "/fxml/cards_dialog.fxml",
            "/fxml/settings_dialog.fxml",
            "/fxml/victory_dialog.fxml",
            "/fxml/steal_dialog.fxml",
            "/fxml/discard_dialog.fxml",
        };
        for (String fxml : fxmls) {
            Parent root = FXMLLoader.load(FxmlLoadTest.class.getResource(fxml));
            assertNotNull(root, "Root null for " + fxml);
        }
    }
}
