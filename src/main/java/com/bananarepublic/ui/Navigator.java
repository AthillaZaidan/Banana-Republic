package com.bananarepublic.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public final class Navigator {
    private static final String CSS_PATH = "/css/style.css";

    private static Stage primary;
    private static StackPane shell;

    private Navigator() {}

    public static void init(Stage stage) {
        primary = stage;
        shell = new StackPane();
        Scene scene = new Scene(shell, 1280, 800);
        applyStylesheet(scene);
        stage.setScene(scene);
    }

    public static Stage primary() {
        return primary;
    }

    public static void goTo(String fxml) {
        Parent root = load(fxml);
        shell.getChildren().setAll(root);
    }

    public static Parent showOverlay(String fxml) {
        Parent overlay = load(fxml);
        shell.getChildren().add(overlay);
        return overlay;
    }

    public static void closeOverlay(Node child) {
        if (child == null || shell == null) return;
        Node n = child;
        while (n != null && n.getParent() != shell) {
            n = n.getParent();
        }
        if (n != null) shell.getChildren().remove(n);
    }

    private static Parent load(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxml));
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + fxml, e);
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
