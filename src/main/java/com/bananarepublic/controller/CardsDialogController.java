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
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.plugin.PluginExperimentCardAdapter;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CardsDialogController {
    @FXML private StackPane root;
    @FXML private HBox cardRow;
    @FXML private Label emptyLabel;

    private VBox selectedCardBox;
    private DevelopmentCard selectedCard;
    private final java.util.Map<VBox, DevelopmentCard> cardMap = new java.util.HashMap<>();

    @FXML
    public void initialize() {
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
        Label footer = new Label(playableNow ? "DAPAT DIMAINKAN" : "BELUM BISA");
        footer.getStyleClass().add("exp-card-footer");
        if (!playableNow) footer.getStyleClass().add("is-passive");
        footer.setMaxWidth(Double.MAX_VALUE);

        box.getChildren().addAll(header, body, footer);
        box.setOnMouseClicked(e -> select(box));
        return box;
    }

    private boolean isPlayableNow(DevelopmentCard card) {
        if (!GameSession.hasEngine()) return false;
        var state = GameSession.engine().getState();
        var turnState = state.getTurnState();
        if (turnState.hasPlayedDevelopmentCard()) return false;
        if (turnState.isNewlyBoughtCard(card.getId()) && !(card instanceof VictoryPointCard)) return false;
        return true;
    }

    private void select(VBox box) {
        if (selectedCardBox != null) selectedCardBox.getStyleClass().remove("is-selected");
        selectedCardBox = box;
        selectedCard = cardMap.get(box);
        if (!box.getStyleClass().contains("is-selected")) {
            box.getStyleClass().add("is-selected");
        }
    }

    @FXML
    private void onBuy() {
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
        if (selectedCard == null) {
            showAlert("Pilih Kartu", "Pilih kartu yang ingin dimainkan.");
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
                case VictoryPointCard vp -> {
                    engine.playDevelopmentCard(playerId, cardId);
                    logEvent(active.getName() + " memainkan Kartu Poin Prestasi Rahasia.");
                }
                case PluginExperimentCardAdapter plugin -> {
                    engine.playDevelopmentCard(playerId, cardId);
                    logEvent(active.getName() + " memainkan kartu eksperimen: " + plugin.getName());
                }
                case KnightCard k -> {
                    String tileId = promptTileSelection(engine.getState());
                    if (tileId == null) return;
                    String victimId = promptVictimSelection(engine.getState(), tileId);
                    engine.playDevelopmentCard(playerId, cardId, tileId, victimId);
                    logEvent(active.getName() + " memainkan Kartu Penjaga.");
                }
                case MonopolyCard m -> {
                    ResourceType target = promptResourceSelection();
                    if (target == null) return;
                    engine.playDevelopmentCard(playerId, cardId, target);
                    logEvent(active.getName() + " memainkan Monopoli Nimon (target: " + target + ").");
                }
                case RoadBuildingCard r -> {
                    List<String> pathIds = pickAutoRoadPaths(engine.getState(), active);
                    if (pathIds.isEmpty()) {
                        showAlert("Gagal", "Tidak ada jalur yang bisa dibangun.");
                        return;
                    }
                    engine.playDevelopmentCard(playerId, cardId, pathIds);
                    logEvent(active.getName() + " memainkan Konstruksi Cepat (" + pathIds.size() + " pipa).");
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
    private void onClose() { close(); }

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

    private String promptTileSelection(GameState state) {
        List<HexTile> tiles = state.getBoard().getTiles().stream()
                .filter(t -> !t.getId().equals(state.getNimonTileId()))
                .toList();
        List<String> tileIds = tiles.stream().map(HexTile::getId).toList();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(tileIds.get(0), tileIds);
        dialog.setTitle("Pindahkan Nimon Ungu");
        dialog.setHeaderText("Pilih petak tujuan Nimon Ungu");
        dialog.setContentText("Petak:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private String promptVictimSelection(GameState state, String tileId) {
        HexTile tile = state.getBoard().getTile(tileId);
        if (tile == null) return null;

        List<Player> victims = new ArrayList<>();
        for (var intersection : tile.getIntersections()) {
            intersection.getBuilding().ifPresent(b -> {
                Player owner = b.getOwner();
                if (owner != null && !owner.equals(state.getCurrentPlayer()) && !victims.contains(owner)) {
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

    private List<String> pickAutoRoadPaths(GameState state, Player player) {
        List<String> result = new ArrayList<>();
        for (var path : state.getBoard().getPaths()) {
            if (result.size() >= 2) break;
            if (path.hasPipe()) continue;
            boolean connected = isConnectedToPlayerNetwork(path, player);
            if (connected) {
                result.add(path.getId());
            }
        }
        return result;
    }

    private boolean isConnectedToPlayerNetwork(com.bananarepublic.model.board.Path path, Player player) {
        var epA = path.getEndpointA();
        var epB = path.getEndpointB();
        return epA.getBuilding().map(b -> b.isOwnedBy(player)).orElse(false)
                || epB.getBuilding().map(b -> b.isOwnedBy(player)).orElse(false)
                || epA.getConnectedPaths().stream()
                        .filter(cp -> cp != path)
                        .flatMap(cp -> cp.getPipe().stream())
                        .anyMatch(pipe -> pipe.isOwnedBy(player))
                || epB.getConnectedPaths().stream()
                        .filter(cp -> cp != path)
                        .flatMap(cp -> cp.getPipe().stream())
                        .anyMatch(pipe -> pipe.isOwnedBy(player));
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
            case KnightCard k -> new CardMeta("knight", "⚔", "KARTU PENJAGA", "Penjaga",
                "Pindahkan Nimon Ungu, lalu curi 1 kartu sumber daya.");
            case RoadBuildingCard r -> new CardMeta("progress", "🛠", "KARTU INOVASI", "Inovasi: Jalur",
                "Bangun 2 Pipa gratis di petak manapun.");
            case MonopolyCard m -> new CardMeta("progress", "🧪", "KARTU INOVASI", "Inovasi: Monopoli",
                "Pilih 1 jenis sumber daya. Semua lawan menyerahkannya.");
            case VictoryPointCard v -> new CardMeta("vp", "📜", "POIN PRESTASI", "Poin Rahasia",
                "Memberikan +1 VP. Tersembunyi hingga akhir.");
            case PluginExperimentCardAdapter p -> new CardMeta("plugin", "🔌", "KARTU EKSPERIMEN", p.getName(),
                p.getDescription());
            default -> new CardMeta("unknown", "❓", "???", "Kartu Misterius", "Efek tidak diketahui.");
        };
    }

    private record CardMeta(String kind, String icon, String header, String name, String desc) {}
}
