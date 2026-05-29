package com.bananarepublic.controller;

import com.bananarepublic.ui.Navigator;
import com.bananarepublic.ui.ResourceIcons;
import com.bananarepublic.ui.Stepper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class TradeDialogController {
    private record Resource(ResourceIcons.Kind kind, String label) {}
    private static final List<Resource> RES = List.of(
        new Resource(ResourceIcons.Kind.WOOD,   "WOOD"),
        new Resource(ResourceIcons.Kind.BRICK,  "BRICK"),
        new Resource(ResourceIcons.Kind.WHEAT,  "WHEAT"),
        new Resource(ResourceIcons.Kind.ORE,    "ORE"),
        new Resource(ResourceIcons.Kind.BANANA, "BANANA")
    );
    private static final int[] OWNED = {2, 1, 2, 1, 1};

    @FXML private StackPane root;
    @FXML private Label tabDomestic;
    @FXML private Label tabMaritime;
    @FXML private HBox giveRow;
    @FXML private HBox receiveRow;
    @FXML private HBox offerToRow;
    @FXML private VBox offerToBlock;
    @FXML private Label maritimeRateLabel;

    @FXML
    public void initialize() {
        for (int i = 0; i < RES.size(); i++) {
            giveRow.getChildren().add(resTile(RES.get(i), OWNED[i], OWNED[i]));
            receiveRow.getChildren().add(resTile(RES.get(i), 0, 9));
        }
        offerToRow.getChildren().addAll(
            offerChip("Stewart", "red",   true),
            offerChip("Kebin",   "gold",  false),
            offerChip("Tara",    "white", true)
        );
    }

    private VBox resTile(Resource r, int owned, int max) {
        VBox tile = new VBox(4);
        tile.getStyleClass().add("res-tile");
        StackPane icon = new StackPane(ResourceIcons.of(r.kind()));
        icon.setMinSize(28, 28); icon.setMaxSize(28, 28);
        Label name = new Label(r.label());
        name.getStyleClass().add("eyebrow");
        Label hold = new Label("Hold: " + owned);
        hold.setStyle("-fx-font-size: 10px; -fx-text-fill: -ink-mute;");
        Stepper stepper = new Stepper(0, 0, max);
        tile.getChildren().addAll(icon, name, hold, stepper);
        return tile;
    }

    private HBox offerChip(String name, String color, boolean selected) {
        HBox chip = new HBox(8);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-padding: 6 12 6 6; -fx-background-radius: 999;"
            + (selected
                ? "-fx-border-color: -p-" + color + "; -fx-border-width: 2; -fx-border-radius: 999;"
                : "-fx-background-color: rgba(0,0,0,0.04); -fx-border-width: 2; -fx-border-color: transparent; -fx-border-radius: 999;"));

        StackPane initial = new StackPane(new Label(String.valueOf(name.charAt(0))));
        initial.getStyleClass().addAll("initial-chip", "pc-" + color);
        initial.setMinSize(24, 24); initial.setMaxSize(24, 24);

        Label l = new Label(name);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        chip.getChildren().addAll(initial, l);
        return chip;
    }

    @FXML
    private void onSelectDomestic() {
        tabDomestic.getStyleClass().setAll("tab", "is-active");
        tabMaritime.getStyleClass().setAll("tab");
        offerToBlock.setVisible(true); offerToBlock.setManaged(true);
        maritimeRateLabel.setVisible(false); maritimeRateLabel.setManaged(false);
    }

    @FXML
    private void onSelectMaritime() {
        tabMaritime.getStyleClass().setAll("tab", "is-active");
        tabDomestic.getStyleClass().setAll("tab");
        offerToBlock.setVisible(false); offerToBlock.setManaged(false);
        maritimeRateLabel.setVisible(true); maritimeRateLabel.setManaged(true);
    }

    @FXML
    private void onSubmit() {
        System.out.println("[Trade] submit");
        close();
    }

    @FXML
    private void onClose() { close(); }

    private void close() {
        Navigator.closeOverlay(root);
    }
}
