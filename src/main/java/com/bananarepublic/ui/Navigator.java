package com.bananarepublic.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;

public final class Navigator {
    private static final String CSS_PATH = "/css/style.css";

    private static Stage primary;

    private Navigator() {}

    public static void init(Stage stage) {
        primary = stage;
    }

    public static Stage primary() {
        return primary;
    }

    public static void goTo(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxml));
            Parent root = loader.load();
            Scene scene = primary.getScene();
            if (scene == null) {
                scene = new Scene(root, 1280, 800);
                applyStylesheet(scene);
                primary.setScene(scene);
            } else {
                scene.setRoot(root);
                applyStylesheet(scene);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + fxml, e);
        }
    }

    public static Stage openModal(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxml));
            Parent root = loader.load();
            Stage dialog = new Stage(StageStyle.TRANSPARENT);
            dialog.initOwner(primary);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(title);
            Scene scene = new Scene(root);
            scene.setFill(null);
            applyStylesheet(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
            return dialog;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load dialog " + fxml, e);
        }
    }

    private static void applyStylesheet(Scene scene) {
        URL css = Navigator.class.getResource(CSS_PATH);
        if (css != null) {
            String href = css.toExternalForm();
            if (!scene.getStylesheets().contains(href)) {
                scene.getStylesheets().add(href);
            }
        }
    }
}
