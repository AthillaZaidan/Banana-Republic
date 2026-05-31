package com.bananarepublic.controller;

import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class BuildCostsDialogController {
    @FXML private StackPane root;

    @FXML
    private void onClose() {
        Navigator.closeOverlay(root);
    }
}
