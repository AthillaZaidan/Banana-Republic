package com.bananarepublic.controller;

import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.service.build.BuildActionType;
import com.bananarepublic.service.build.BuildCostProvider;
import com.bananarepublic.ui.Navigator;
import com.bananarepublic.ui.ResourceIcons;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class BuildCostsDialogController {
    @FXML private StackPane root;
    @FXML private VBox costsBox;

    private final BuildCostProvider buildCostProvider = new BuildCostProvider();

    @FXML
    public void initialize() {
        costsBox.getChildren().setAll(
                createCostRow("PIPE", "Pipa Transportasi", "Bangun jalur baru antar titik.", BuildActionType.PIPE),
                createCostRow("POST", "Pos Pantau", "Bangun pos baru di simpul kosong yang valid.", BuildActionType.MONITORING_POST),
                createCostRow("LAB", "Laboratorium", "Upgrade pos pantau yang sudah kamu miliki.", BuildActionType.LABORATORY),
                createCostRow("CARD", "Kartu Temuan", "Beli satu kartu temuan dari deck aktif.", BuildActionType.EXPERIMENT_CARD)
        );
    }

    @FXML
    private void onClose() {
        Navigator.closeOverlay(root);
    }

    private HBox createCostRow(String badgeText, String title, String subtitle, BuildActionType actionType) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("card-dark", "build-cost-row");

        StackPane badge = new StackPane(new Label(badgeText));
        badge.getStyleClass().add("build-cost-row__badge");
        badge.getChildren().getFirst().getStyleClass().add("build-cost-row__badge-text");

        VBox copy = new VBox(3);
        copy.setMinWidth(170);
        HBox.setHgrow(copy, Priority.ALWAYS);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("build-cost-row__title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("build-cost-row__subtitle");
        subtitleLabel.setWrapText(true);
        copy.getChildren().addAll(titleLabel, subtitleLabel);

        HBox costs = createCostChips(buildCostProvider.getCost(actionType));
        row.getChildren().addAll(badge, copy, costs);
        return row;
    }

    private HBox createCostChips(ResourceInventory cost) {
        HBox chips = new HBox(18);
        chips.getStyleClass().add("cost-items-row");
        chips.setAlignment(Pos.CENTER_RIGHT);
        for (ResourceType type : ResourceType.values()) {
            int amount = cost.getAmount(type);
            if (amount > 0) {
                chips.getChildren().add(createCostChip(type, amount));
            }
        }
        return chips;
    }

    private HBox createCostChip(ResourceType type, int amount) {
        HBox chip = new HBox(8);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("cost-item");

        Group icon = ResourceIcons.of(toIconKind(type));
        double scale = iconScale(type);
        icon.setScaleX(scale);
        icon.setScaleY(scale);

        StackPane iconSlot = new StackPane(icon);
        iconSlot.getStyleClass().add("cost-item__icon-slot");

        Label amountLabel = new Label("x" + amount);
        amountLabel.getStyleClass().add("cost-item__value");

        chip.getChildren().addAll(iconSlot, amountLabel);
        return chip;
    }

    private ResourceIcons.Kind toIconKind(ResourceType type) {
        return switch (type) {
            case WOOD -> ResourceIcons.Kind.WOOD;
            case BRICK -> ResourceIcons.Kind.BRICK;
            case WHEAT -> ResourceIcons.Kind.WHEAT;
            case ORE -> ResourceIcons.Kind.ORE;
            case BANANA -> ResourceIcons.Kind.BANANA;
        };
    }

    private double iconScale(ResourceType type) {
        return switch (type) {
            case WOOD -> 1.55;
            case BRICK -> 1.5;
            case WHEAT -> 1.55;
            case ORE -> 1.65;
            case BANANA -> 1.7;
        };
    }
}
