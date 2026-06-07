package com.bananarepublic.controller;

import com.bananarepublic.engine.GameEngine;
import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.card.DevelopmentCard;
import com.bananarepublic.model.card.KnightCard;
import com.bananarepublic.model.card.MonopolyCard;
import com.bananarepublic.model.card.RoadBuildingCard;
import com.bananarepublic.model.card.VictoryPointCard;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.plugin.PluginExperimentCardAdapter;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.AudioEngine;
import com.bananarepublic.ui.Navigator;
import com.bananarepublic.ui.ResourceIcons;
import com.bananarepublic.service.build.BuildActionType;
import com.bananarepublic.service.build.BuildCostProvider;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CardsDialogController {
    @FXML private StackPane root;
    @FXML private HBox cardRow;
    @FXML private Label emptyLabel;
    @FXML private Label buyHintLabel;
    @FXML private Button buyBtn;
    @FXML private Button playBtn;

    private VBox selectedCardBox;
    private DevelopmentCard selectedCard;
    private final java.util.Map<VBox, DevelopmentCard> cardMap = new java.util.HashMap<>();
    private final BuildCostProvider buildCostProvider = new BuildCostProvider();

    @FXML
    public void initialize() {
        if (buyBtn != null) {
            buyBtn.setGraphic(createBuyButtonGraphic());
            buyBtn.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            buyBtn.setGraphicTextGap(10);
        }
        refreshCards();
    }

    private void refreshCards() {
        cardRow.getChildren().clear();
        cardMap.clear();
        selectedCardBox = null;
        selectedCard = null;

        if (!GameSession.hasEngine()) {
            emptyLabel.setText("Tidak ada engine aktif.");
            emptyLabel.setVisible(true);
            return;
        }

        Player active = GameSession.engine().getState().getCurrentPlayer();
        List<DevelopmentCard> hand = active.getHandCards();

        if (hand.isEmpty()) {
            emptyLabel.setText("Belum ada kartu di tangan. Beli kartu temuan!");
            emptyLabel.setVisible(true);
            refreshBuyButton();
            refreshPlayButton();
            return;
        }

        emptyLabel.setVisible(false);
        boolean first = true;
        for (DevelopmentCard card : hand) {
            VBox box = buildCard(card, first);
            cardMap.put(box, card);
            cardRow.getChildren().add(box);
            if (first) {
                selectedCardBox = box;
                selectedCard = card;
            }
            first = false;
        }
        refreshBuyButton();
        refreshPlayButton();
    }

    private VBox buildCard(DevelopmentCard card, boolean isSelected) {
        CardMeta meta = getMeta(card);

        VBox box = new VBox();
        box.getStyleClass().add("exp-card");
        if (isSelected) box.getStyleClass().add("is-selected");

        Label header = new Label(meta.header);
        header.getStyleClass().addAll("exp-card-header", "exp-card-header-" + meta.kind);
        header.setMaxWidth(Double.MAX_VALUE);

        VBox body = new VBox(8);
        body.setAlignment(Pos.CENTER);
        body.setStyle("-fx-padding: 12;");
        StackPane iconBox = new StackPane(new Label(meta.icon));
        iconBox.setMinSize(56, 56); iconBox.setMaxSize(56, 56);
        iconBox.setStyle("-fx-background-color: #fff8e1; -fx-background-radius: 12;"
            + " -fx-border-color: -parchment-line; -fx-border-radius: 12;");
        ((Label) iconBox.getChildren().get(0)).setStyle("-fx-font-size: 32px;");

        Label name = new Label(meta.name);
        name.setStyle("-fx-font-weight: 800; -fx-font-size: 13px;");

        Label desc = new Label(meta.desc);
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: -ink-mute;");
        desc.setAlignment(Pos.CENTER);
        desc.setMaxWidth(140);

        body.getChildren().addAll(iconBox, name, desc);

        boolean playableNow = isPlayableNow(card);
        String footerText = card instanceof VictoryPointCard
                ? "TERSEMBUNYI HINGGA AKHIR"
                : playableNow ? "DAPAT DIMAINKAN" : "BELUM BISA";
        Label footer = new Label(footerText);
        footer.getStyleClass().add("exp-card-footer");
        if (!playableNow) footer.getStyleClass().add("is-passive");
        footer.setMaxWidth(Double.MAX_VALUE);

        box.getChildren().addAll(header, body, footer);
        box.setOnMouseClicked(e -> { AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK); select(box); });
        return box;
    }

    private boolean isPlayableNow(DevelopmentCard card) {
        if (!GameSession.hasEngine()) return false;
        if (card instanceof VictoryPointCard) return false;
        var state = GameSession.engine().getState();
        var turnState = state.getTurnState();
        if (turnState.hasPlayedDevelopmentCard()) return false;
        if (turnState.isNewlyBoughtCard(card.getId())) return false;
        return true;
    }

    private void select(VBox box) {
        if (selectedCardBox != null) selectedCardBox.getStyleClass().remove("is-selected");
        selectedCardBox = box;
        selectedCard = cardMap.get(box);
        if (!box.getStyleClass().contains("is-selected")) {
            box.getStyleClass().add("is-selected");
        }
        refreshPlayButton();
    }

    @FXML
    private void onBuy() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        if (!GameSession.hasEngine()) {
            showAlert("Error", "Tidak ada engine aktif.");
            return;
        }
        GameEngine engine = GameSession.engine();
        Player active = engine.getState().getCurrentPlayer();
        try {
            engine.buyDevelopmentCard(active.getId());
            logEvent(active.getName() + " membeli Kartu Temuan.");
            refreshCards();
            refreshGameController();
        } catch (RuntimeException ex) {
            showAlert("Gagal Membeli", ex.getMessage());
        }
    }

    @FXML
    private void onPlay() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        if (selectedCard == null) {
            showAlert("Pilih Kartu", "Pilih kartu yang ingin dimainkan.");
            return;
        }
        if (selectedCard instanceof VictoryPointCard) {
            showAlert("Kartu Rahasia", "Kartu Poin Prestasi Rahasia tetap tersembunyi di tangan sampai akhir permainan atau saat kamu menang.");
            refreshPlayButton();
            return;
        }
        if (!GameSession.hasEngine()) {
            showAlert("Error", "Tidak ada engine aktif.");
            return;
        }

        GameEngine engine = GameSession.engine();
        Player active = engine.getState().getCurrentPlayer();
        String playerId = active.getId();
        String cardId = selectedCard.getId();

        try {
            switch (selectedCard) {
                case PluginExperimentCardAdapter plugin -> {
                    engine.playDevelopmentCard(playerId, cardId);
                    logEvent(active.getName() + " memainkan kartu eksperimen: " + plugin.getName());
                }
                case KnightCard k -> {
                    beginKnightPlacement(engine, active, cardId);
                    return;
                }
                case MonopolyCard m -> {
                    ResourceType target = promptResourceSelection();
                    if (target == null) return;
                    int before = active.getResourceAmount(target);
                    engine.playDevelopmentCard(playerId, cardId, target);
                    int gained = active.getResourceAmount(target) - before;
                    logEvent(active.getName() + " memainkan Monopoli Nimon pada "
                            + resourceLabel(target) + " dan mengambil " + gained + " " + resourceLabel(target) + ".");
                }
                case RoadBuildingCard r -> {
                    beginRoadBuildingPlacement(engine, active, cardId);
                    return;
                }
                default -> {
                    showAlert("Error", "Tipe kartu tidak dikenal.");
                    return;
                }
            }
            refreshCards();
            refreshGameController();
            checkVictoryAfterPlay();
        } catch (RuntimeException ex) {
            showAlert("Gagal Memainkan Kartu", ex.getMessage());
        }
    }

    @FXML
    private void onClose() { AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK); close(); }

    private void close() {
        Navigator.closeOverlay(root);
    }

    private void refreshGameController() {
        var gc = GameSession.getGameController();
        if (gc != null) gc.refresh();
    }

    private void checkVictoryAfterPlay() {
        if (!GameSession.hasEngine()) return;
        if (GameSession.engine().getState().isGameOver()) {
            close();
            Navigator.showOverlay("/fxml/victory_dialog.fxml");
        }
    }

    private void logEvent(String message) {
        var gc = GameSession.getGameController();
        if (gc != null) {
            gc.log("[Card] " + message);
        }
    }

    private void beginKnightPlacement(GameEngine engine, Player active, String cardId) {
        GameController gameController = GameSession.getGameController();
        if (gameController == null) {
            showAlert("Error", "Board controller tidak tersedia.");
            return;
        }

        List<String> tileIds = engine.getState().getBoard().getTiles().stream()
                .filter(tile -> !tile.getId().equals(engine.getState().getNimonTileId()))
                .map(HexTile::getId)
                .toList();
        if (tileIds.isEmpty()) {
            showAlert("Gagal", "Tidak ada petak tujuan yang valid.");
            return;
        }

        close();
        gameController.beginTileSelection(
                "Click a highlighted tile to move Nimon Ungu with the Knight card.",
                tileIds,
                tileId -> {
                    try {
                        GameState state = GameSession.engine().getState();
                        String victimId = promptVictimSelection(state, tileId);
                        if (victimId == null && hasValidVictimForTile(state, tileId)) {
                            refreshGameController();
                            return;
                        }
                        GameSession.engine().playDevelopmentCard(active.getId(), cardId, tileId, victimId);
                        String detail = victimId == null
                                ? "."
                                : " dan mencuri 1 kartu sumber daya dari "
                                + GameSession.engine().getState().getPlayerById(victimId).getName() + ".";
                        logEvent(active.getName() + " memainkan Kartu Penjaga, memindahkan Nimon ke "
                                + tileId + detail);
                        refreshGameController();
                        checkVictoryAfterPlay();
                    } catch (RuntimeException ex) {
                        showAlert("Gagal Memainkan Kartu", ex.getMessage());
                        refreshGameController();
                    }
                }
        );
    }

    private String promptVictimSelection(GameState state, String tileId) {
        HexTile tile = state.getBoard().getTile(tileId);
        if (tile == null) return null;

        List<Player> victims = new ArrayList<>();
        for (var intersection : tile.getIntersections()) {
            intersection.getBuilding().ifPresent(b -> {
                Player owner = b.getOwner();
                if (owner != null
                        && !owner.equals(state.getCurrentPlayer())
                        && owner.getTotalResourceCards() > 0
                        && !victims.contains(owner)) {
                    victims.add(owner);
                }
            });
        }

        if (victims.isEmpty()) return null;

        List<String> names = victims.stream().map(Player::getName).toList();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Curi Kartu");
        dialog.setHeaderText("Pilih pemain yang akan dicuri");
        dialog.setContentText("Pemain:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return null;

        String chosenName = result.get();
        return victims.stream()
                .filter(p -> p.getName().equals(chosenName))
                .findFirst()
                .map(Player::getId)
                .orElse(null);
    }

    private boolean hasValidVictimForTile(GameState state, String tileId) {
        HexTile tile = state.getBoard().getTile(tileId);
        if (tile == null) {
            return false;
        }
        Player active = state.getCurrentPlayer();
        return tile.getIntersections().stream()
                .map(intersection -> intersection.getBuilding().orElse(null))
                .filter(building -> building != null)
                .map(building -> building.getOwner())
                .anyMatch(owner -> !owner.equals(active) && owner.getTotalResourceCards() > 0);
    }

    private ResourceType promptResourceSelection() {
        List<String> names = java.util.Arrays.stream(ResourceType.values())
                .map(ResourceType::name)
                .toList();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Monopoli Nimon");
        dialog.setHeaderText("Pilih jenis sumber daya target");
        dialog.setContentText("Sumber daya:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return null;
        try {
            return ResourceType.valueOf(result.get());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void refreshBuyButton() {
        if (buyBtn == null) {
            return;
        }
        if (!GameSession.hasEngine()) {
            buyBtn.setDisable(true);
            if (buyHintLabel != null) {
                buyHintLabel.setText("Tidak ada engine aktif.");
            }
            return;
        }

        GameEngine engine = GameSession.engine();
        Player active = engine.getState().getCurrentPlayer();
        String blockReason = engine.getDevelopmentCardPurchaseBlockReason(active.getId());
        buyBtn.setDisable(blockReason != null);
        if (buyHintLabel != null) {
            buyHintLabel.setText(blockReason == null ? "" : blockReason);
        }
    }

    private void refreshPlayButton() {
        if (playBtn == null) {
            return;
        }
        playBtn.setDisable(selectedCard == null || !isPlayableNow(selectedCard));
    }

    private HBox createBuyButtonGraphic() {
        HBox graphic = new HBox(8);
        graphic.setAlignment(Pos.CENTER_LEFT);
        graphic.getStyleClass().add("card-buy-costs");

        ResourceInventory cost = buildCostProvider.getCost(BuildActionType.EXPERIMENT_CARD);
        for (ResourceType type : ResourceType.values()) {
            int amount = cost.getAmount(type);
            if (amount > 0) {
                graphic.getChildren().add(createBuyCostItem(type, amount));
            }
        }
        return graphic;
    }

    private HBox createBuyCostItem(ResourceType type, int amount) {
        HBox item = new HBox(4);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("card-buy-cost-item");

        Group icon = ResourceIcons.of(toIconKind(type));
        icon.setScaleX(1.1);
        icon.setScaleY(1.1);

        StackPane iconSlot = new StackPane(icon);
        iconSlot.getStyleClass().add("card-buy-cost-item__icon");

        Label countLabel = new Label("x" + amount);
        countLabel.getStyleClass().add("card-buy-cost-item__value");

        item.getChildren().addAll(iconSlot, countLabel);
        return item;
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

    private String resourceLabel(ResourceType type) {
        return switch (type) {
            case WOOD -> "Kayu";
            case BRICK -> "Batu Bata";
            case WHEAT -> "Gandum";
            case ORE -> "Bijih";
            case BANANA -> "Pisang";
        };
    }

    private void beginRoadBuildingPlacement(GameEngine engine, Player player, String cardId) {
        GameController gameController = GameSession.getGameController();
        if (gameController == null) {
            showAlert("Error", "Board controller tidak tersedia.");
            return;
        }

        List<String> firstOptions = validRoadBuildingPathIds(engine.getState(), player, List.of());
        if (firstOptions.isEmpty()) {
            showAlert("Gagal", "Tidak ada jalur yang bisa dibangun.");
            return;
        }

        close();
        requestRoadBuildingPath(gameController, player, cardId, new ArrayList<>(), firstOptions, true);
    }

    private void requestRoadBuildingPath(
            GameController gameController,
            Player player,
            String cardId,
            List<String> selectedPathIds,
            List<String> options,
            boolean firstPick
    ) {
        String prompt = firstPick
                ? "Click a highlighted path for the first free pipe."
                : "Click a highlighted path for the second free pipe, or right-click to finish.";

        Runnable cancelAction = firstPick
                ? this::refreshGameController
                : () -> finalizeRoadBuildingSelection(player, cardId, selectedPathIds);

        gameController.beginPathSelection(prompt, options, pathId -> {
            List<String> updated = new ArrayList<>(selectedPathIds);
            updated.add(pathId);

            List<String> nextOptions = validRoadBuildingPathIds(GameSession.engine().getState(), player, updated);
            if (firstPick && !nextOptions.isEmpty()) {
                requestRoadBuildingPath(gameController, player, cardId, updated, nextOptions, false);
                return;
            }
            finalizeRoadBuildingSelection(player, cardId, updated);
        }, cancelAction);
    }

    private void finalizeRoadBuildingSelection(Player player, String cardId, List<String> pathIds) {
        if (pathIds.isEmpty()) {
            refreshGameController();
            return;
        }
        try {
            GameSession.engine().playDevelopmentCard(player.getId(), cardId, pathIds);
            logEvent(player.getName() + " memainkan Konstruksi Cepat (" + pathIds.size() + " pipa).");
            refreshGameController();
            checkVictoryAfterPlay();
        } catch (RuntimeException ex) {
            showAlert("Gagal Memainkan Kartu", ex.getMessage());
            refreshGameController();
        }
    }

    private List<String> validRoadBuildingPathIds(GameState state, Player player, List<String> plannedPathIds) {
        return state.getBoard().getPaths().stream()
                .filter(path -> !plannedPathIds.contains(path.getId()))
                .filter(path -> canBuildRoadBuildingPipe(path, player, Set.copyOf(plannedPathIds)))
                .map(com.bananarepublic.model.board.Path::getId)
                .toList();
    }

    private boolean canBuildRoadBuildingPipe(com.bananarepublic.model.board.Path path, Player player, Set<String> plannedPathIds) {
        if (path.hasPipe()) {
            return false;
        }
        return canExtendFrom(path.getEndpointA(), path, player, plannedPathIds)
                || canExtendFrom(path.getEndpointB(), path, player, plannedPathIds);
    }

    private boolean canExtendFrom(
            com.bananarepublic.model.board.Intersection intersection,
            com.bananarepublic.model.board.Path targetPath,
            Player player,
            Set<String> plannedPathIds
    ) {
        if (intersection.getBuilding().map(building -> building.isOwnedBy(player)).orElse(false)) {
            return true;
        }

        if (intersection.getBuilding().map(building -> !building.isOwnedBy(player)).orElse(false)) {
            return false;
        }

        return intersection.getConnectedPaths().stream()
                .filter(path -> path != targetPath)
                .anyMatch(path -> plannedPathIds.contains(path.getId())
                        || path.getPipe().map(pipe -> pipe.isOwnedBy(player)).orElse(false));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private CardMeta getMeta(DevelopmentCard card) {
        return switch (card) {
            case KnightCard k -> new CardMeta("knight", "KNT", "KARTU PENJAGA", "Penjaga",
                "Pindahkan Nimon Ungu, lalu curi 1 kartu sumber daya.");
            case RoadBuildingCard r -> new CardMeta("progress", "JAL", "KARTU INOVASI", "Inovasi: Jalur",
                "Bangun 2 Pipa gratis di petak manapun.");
            case MonopolyCard m -> new CardMeta("progress", "MON", "KARTU INOVASI", "Inovasi: Monopoli",
                "Pilih 1 jenis sumber daya. Semua lawan menyerahkannya.");
            case VictoryPointCard v -> new CardMeta("vp", "VP", "POIN PRESTASI", "Poin Rahasia",
                "Memberikan +1 VP. Tersembunyi hingga akhir.");
            case PluginExperimentCardAdapter p -> new CardMeta("plugin", "PLG", "KARTU EKSPERIMEN", p.getName(),
                p.getDescription());
            default -> new CardMeta("unknown", "?", "???", "Kartu Misterius", "Efek tidak diketahui.");
        };
    }

    private record CardMeta(String kind, String icon, String header, String name, String desc) {}
}
