package com.bananarepublic.controller;

import com.bananarepublic.engine.GameEngine;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.player.PlayerColor;
import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.service.trade.MaritimeTradeRequest;
import com.bananarepublic.service.trade.TradeOffer;
import com.bananarepublic.service.trade.TradeResult;
import com.bananarepublic.ui.AudioEngine;
import com.bananarepublic.ui.GameIcons;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.Navigator;
import com.bananarepublic.ui.ResourceIcons;
import com.bananarepublic.ui.Stepper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TradeDialogController {
    private record Resource(ResourceType type, ResourceIcons.Kind kind, String label) {}

    private static final List<Resource> RESOURCES = List.of(
            new Resource(ResourceType.WOOD, ResourceIcons.Kind.WOOD, "WOOD"),
            new Resource(ResourceType.BRICK, ResourceIcons.Kind.BRICK, "BRICK"),
            new Resource(ResourceType.WHEAT, ResourceIcons.Kind.WHEAT, "WHEAT"),
            new Resource(ResourceType.ORE, ResourceIcons.Kind.ORE, "ORE"),
            new Resource(ResourceType.BANANA, ResourceIcons.Kind.BANANA, "BANANA")
    );

    @FXML private StackPane root;
    @FXML private Label tabDomestic;
    @FXML private Label tabMaritime;
    @FXML private Label tradeStateLabel;
    @FXML private HBox giveRow;
    @FXML private HBox receiveRow;
    @FXML private HBox offerToRow;
    @FXML private VBox offerToBlock;
    @FXML private Label maritimeRateLabel;
    @FXML private Button closeBtn;
    @FXML private Button rejectBtn;
    @FXML private Button acceptBtn;
    @FXML private Button submitBtn;
    @FXML private Pane tradeArrowIcon;

    private final Map<ResourceType, Stepper> giveSteppers = new EnumMap<>(ResourceType.class);
    private final Map<ResourceType, Stepper> receiveSteppers = new EnumMap<>(ResourceType.class);
    private final List<HBox> targetChips = new ArrayList<>();
    private final List<Player> candidateTargets = new ArrayList<>();

    private boolean maritimeMode;
    private Player selectedTarget;
    private Player currentComposer;
    private TradeOffer pendingOffer;

    @FXML
    public void initialize() {
        tradeArrowIcon.getChildren().setAll(GameIcons.trade());
        if (!GameSession.hasEngine()) {
            close();
            return;
        }

        pendingOffer = GameSession.engine().getPendingTradeOffer();
        if (pendingOffer != null) {
            onSelectDomestic();
            loadPendingOfferMode();
        } else {
            loadComposeMode();
        }
    }

    private VBox buildResourceTile(Resource resource, int owned, Stepper stepper) {
        VBox tile = new VBox(4);
        tile.getStyleClass().add("res-tile");
        StackPane icon = new StackPane(ResourceIcons.of(resource.kind()));
        icon.setMinSize(28, 28);
        icon.setMaxSize(28, 28);

        Label name = new Label(resource.label());
        name.getStyleClass().add("eyebrow");

        Label hold = new Label("Hold: " + owned);
        hold.setStyle("-fx-font-size: 10px; -fx-text-fill: -ink-mute;");

        tile.getChildren().addAll(icon, name, hold, stepper);
        return tile;
    }

    private HBox buildOfferChip(Player player, boolean selected) {
        String color = cssColor(player.getColor());
        HBox chip = new HBox(8);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle(styleForChip(color, selected));

        StackPane initial = new StackPane(new Label(String.valueOf(player.getName().charAt(0))));
        initial.getStyleClass().addAll("initial-chip", "pc-" + color);
        initial.setMinSize(24, 24);
        initial.setMaxSize(24, 24);

        Label name = new Label(player.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        chip.getChildren().addAll(initial, name);
        chip.setOnMouseClicked(e -> { AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK); selectTarget(chip, player); });
        return chip;
    }

    @FXML
    private void onSelectDomestic() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        maritimeMode = false;
        tabDomestic.getStyleClass().setAll("tab", "is-active");
        tabMaritime.getStyleClass().setAll("tab");
        offerToBlock.setVisible(pendingOffer == null);
        offerToBlock.setManaged(pendingOffer == null);
        maritimeRateLabel.setVisible(false);
        maritimeRateLabel.setManaged(false);
        refreshTargetsAndRate();
    }

    @FXML
    private void onSelectMaritime() {
        if (pendingOffer != null) {
            return;
        }
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        maritimeMode = true;
        tabMaritime.getStyleClass().setAll("tab", "is-active");
        tabDomestic.getStyleClass().setAll("tab");
        offerToBlock.setVisible(false);
        offerToBlock.setManaged(false);
        maritimeRateLabel.setVisible(true);
        maritimeRateLabel.setManaged(true);
        refreshTargetsAndRate();
    }

    @FXML
    private void onSubmit() {
        GameEngine engine = GameSession.engine();
        try {
            TradeResult result;
            if (maritimeMode) {
                ResourceType offeredType = singleType(giveSteppers);
                ResourceType requestedType = singleType(receiveSteppers);
                if (offeredType == null || requestedType == null) {
                    throw new IllegalArgumentException("Select exactly one offered and one requested resource type.");
                }
                int offeredAmount = giveSteppers.get(offeredType).valueProperty().get();
                result = engine.submitMaritimeTrade(new MaritimeTradeRequest(
                        engine.getState().getCurrentPlayer().getId(),
                        offeredType,
                        offeredAmount,
                        requestedType
                ));
            } else {
                ResourceInventory offered = toInventory(giveSteppers);
                ResourceInventory requested = toInventory(receiveSteppers);
                if (pendingOffer != null) {
                    Player responder = engine.getState().getPlayerById(pendingOffer.getResponderPlayerId());
                    Player proposer = engine.getState().getPlayerById(pendingOffer.getProposerPlayerId());
                    result = engine.counterDomesticTrade(responder.getId(), new TradeOffer(
                            responder.getId(),
                            proposer.getId(),
                            offered,
                            requested
                    ));
                } else {
                    if (selectedTarget == null) {
                        throw new IllegalArgumentException("Select a domestic trade target.");
                    }
                    String activeId = engine.getState().getCurrentPlayer().getId();
                    result = engine.submitDomesticTrade(new TradeOffer(
                            activeId, selectedTarget.getId(), offered, requested
                    ));
                }
            }

            AudioEngine.get().playSfx(AudioEngine.Sfx.TRADE);
            GameController gameController = GameSession.getGameController();
            if (gameController != null) {
                gameController.log("[Trade] " + result.getMessage());
                gameController.refresh();
            }

            if (result.getPendingOffer() != null) {
                pendingOffer = result.getPendingOffer();
                loadPendingOfferMode();
            } else {
                close();
            }
        } catch (RuntimeException ex) {
            maritimeRateLabel.setVisible(true);
            maritimeRateLabel.setManaged(true);
            maritimeRateLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onAccept() {
        if (pendingOffer == null) {
            return;
        }

        try {
            TradeResult result = GameSession.engine().acceptDomesticTrade(pendingOffer.getResponderPlayerId());
            GameController gameController = GameSession.getGameController();
            if (gameController != null) {
                gameController.log("[Trade] " + result.getMessage());
                gameController.refresh();
            }
            close();
        } catch (RuntimeException ex) {
            maritimeRateLabel.setVisible(true);
            maritimeRateLabel.setManaged(true);
            maritimeRateLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onReject() {
        if (pendingOffer == null) {
            return;
        }

        try {
            TradeResult result = GameSession.engine().rejectDomesticTrade(pendingOffer.getResponderPlayerId());
            GameController gameController = GameSession.getGameController();
            if (gameController != null) {
                gameController.log("[Trade] " + result.getMessage());
                gameController.refresh();
            }
            close();
        } catch (RuntimeException ex) {
            maritimeRateLabel.setVisible(true);
            maritimeRateLabel.setManaged(true);
            maritimeRateLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onClose() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        close();
    }

    private void refreshTargetsAndRate() {
        GameEngine engine = GameSession.engine();
        if (pendingOffer != null) {
            selectedTarget = engine.getState().getPlayerById(pendingOffer.getProposerPlayerId());
            return;
        }

        ResourceType offeredType = singleType(giveSteppers);
        if (offeredType != null) {
            int ratio = engine.getBestMaritimeRatio(currentComposer.getId(), offeredType);
            maritimeRateLabel.setText("Maritime trade rate: " + ratio + ":1");
        } else {
            maritimeRateLabel.setText("Maritime trade requires exactly one offered resource type.");
        }

        ResourceInventory requested = toInventory(receiveSteppers);
        Player active = engine.getState().getCurrentPlayer();
        candidateTargets.clear();
        targetChips.clear();
        offerToRow.getChildren().clear();

        for (Player player : engine.getState().getPlayers()) {
            if (player.equals(active)) {
                continue;
            }
            if (player.hasResources(requested)) {
                candidateTargets.add(player);
            }
        }

        boolean first = true;
        for (Player player : candidateTargets) {
            HBox chip = buildOfferChip(player, first);
            if (first) {
                selectedTarget = player;
                first = false;
            }
            targetChips.add(chip);
            offerToRow.getChildren().add(chip);
        }

        if (candidateTargets.isEmpty()) {
            selectedTarget = null;
        }
    }

    private void loadComposeMode() {
        pendingOffer = null;
        tabMaritime.setDisable(false);
        setStateLabel(null);
        offerToBlock.setVisible(true);
        offerToBlock.setManaged(true);
        closeBtn.setText("CANCEL");
        rejectBtn.setVisible(false);
        rejectBtn.setManaged(false);
        acceptBtn.setVisible(false);
        acceptBtn.setManaged(false);
        submitBtn.setText("SUBMIT OFFER");
        rebuildResourceTiles(GameSession.engine().getState().getCurrentPlayer(), new ResourceInventory(), new ResourceInventory());
        onSelectDomestic();
    }

    private void loadPendingOfferMode() {
        GameEngine engine = GameSession.engine();
        Player proposer = engine.getState().getPlayerById(pendingOffer.getProposerPlayerId());
        Player responder = engine.getState().getPlayerById(pendingOffer.getResponderPlayerId());

        maritimeMode = false;
        tabMaritime.getStyleClass().setAll("tab");
        tabDomestic.getStyleClass().setAll("tab", "is-active");
        tabMaritime.setDisable(true);
        offerToBlock.setVisible(false);
        offerToBlock.setManaged(false);
        closeBtn.setText("CLOSE");
        rejectBtn.setVisible(true);
        rejectBtn.setManaged(true);
        acceptBtn.setVisible(true);
        acceptBtn.setManaged(true);
        submitBtn.setText("SUBMIT COUNTER");
        setStateLabel(responder.getName() + " is responding to " + proposer.getName()
                + ": give " + inventoryText(pendingOffer.getOffered())
                + " for " + inventoryText(pendingOffer.getRequested()) + ".");
        rebuildResourceTiles(responder, pendingOffer.getRequested(), pendingOffer.getOffered());
        refreshTargetsAndRate();
    }

    private void rebuildResourceTiles(Player composer, ResourceInventory initialGive, ResourceInventory initialReceive) {
        currentComposer = composer;
        giveRow.getChildren().clear();
        receiveRow.getChildren().clear();
        giveSteppers.clear();
        receiveSteppers.clear();

        for (Resource resource : RESOURCES) {
            int owned = composer.getResourceAmount(resource.type());
            Stepper giveStepper = new Stepper(Math.min(initialGive.getAmount(resource.type()), owned), 0, owned);
            giveStepper.valueProperty().addListener((obs, oldV, newV) -> refreshTargetsAndRate());
            giveSteppers.put(resource.type(), giveStepper);
            giveRow.getChildren().add(buildResourceTile(resource, owned, giveStepper));

            Stepper receiveStepper = new Stepper(initialReceive.getAmount(resource.type()), 0, 19);
            receiveStepper.valueProperty().addListener((obs, oldV, newV) -> refreshTargetsAndRate());
            receiveSteppers.put(resource.type(), receiveStepper);
            receiveRow.getChildren().add(buildResourceTile(resource, 0, receiveStepper));
        }
    }

    private void setStateLabel(String text) {
        boolean show = text != null && !text.isBlank();
        tradeStateLabel.setVisible(show);
        tradeStateLabel.setManaged(show);
        if (show) {
            tradeStateLabel.setText(text);
        }
    }

    private String inventoryText(ResourceInventory inventory) {
        List<String> tokens = new ArrayList<>();
        for (ResourceType type : ResourceType.values()) {
            int amount = inventory.getAmount(type);
            if (amount > 0) {
                tokens.add(amount + " " + type.name());
            }
        }
        return tokens.isEmpty() ? "nothing" : String.join(", ", tokens);
    }

    private void selectTarget(HBox selectedChip, Player selectedPlayer) {
        selectedTarget = selectedPlayer;
        for (int i = 0; i < targetChips.size(); i++) {
            HBox chip = targetChips.get(i);
            Player player = candidateTargets.get(i);
            chip.setStyle(styleForChip(cssColor(player.getColor()), chip == selectedChip));
        }
    }

    private static String styleForChip(String color, boolean selected) {
        return "-fx-padding: 6 12 6 6; -fx-background-radius: 999;"
                + (selected
                ? "-fx-border-color: -p-" + color + "; -fx-border-width: 2; -fx-border-radius: 999;"
                : "-fx-background-color: rgba(0,0,0,0.04); -fx-border-width: 2; -fx-border-color: transparent; -fx-border-radius: 999;");
    }

    private static ResourceInventory toInventory(Map<ResourceType, Stepper> steppers) {
        ResourceInventory inventory = new ResourceInventory();
        for (Map.Entry<ResourceType, Stepper> entry : steppers.entrySet()) {
            int amount = entry.getValue().valueProperty().get();
            if (amount > 0) {
                inventory.add(entry.getKey(), amount);
            }
        }
        return inventory;
    }

    private static ResourceType singleType(Map<ResourceType, Stepper> steppers) {
        ResourceType found = null;
        for (Map.Entry<ResourceType, Stepper> entry : steppers.entrySet()) {
            if (entry.getValue().valueProperty().get() > 0) {
                if (found != null) {
                    return null;
                }
                found = entry.getKey();
            }
        }
        return found;
    }

    private static String cssColor(PlayerColor color) {
        return switch (color) {
            case RED -> "red";
            case BLUE -> "blue";
            case YELLOW -> "gold";
            case GREEN -> "white";
        };
    }

    private void close() {
        Navigator.closeOverlay(root);
    }
}
