package com.bananarepublic;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainController {
    @FXML
    private Label messageLabel;

    @FXML
    private void handleButtonClick() {
        messageLabel.setText("Banana Republic is ready.");
    }
}
