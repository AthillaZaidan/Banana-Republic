package com.bananarepublic.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class CardsDialogController {
    private record CardData(String kind, String icon, String headerLabel,
                            String name, String desc, boolean playable) {}

    @FXML private HBox cardRow;

    private VBox selected;

    @FXML
    public void initialize() {
        List<CardData> cards = List.of(
            new CardData("knight",   "⚔", "KARTU PENJAGA", "Penjaga",
                "Pindahkan Nimon Ungu, lalu curi 1 kartu sumber daya.", true),
            new CardData("progress", "🛠", "KARTU INOVASI", "Inovasi: Jalur",
                "Bangun 2 Pipa gratis di petak manapun.", true),
            new CardData("progress", "🧪", "KARTU INOVASI", "Inovasi: Monopoli",
                "Pilih 1 jenis sumber. Semua lawan menyerahkannya.", true),
            new CardData("vp",       "📜", "POIN PRESTASI", "Poin Rahasia",
                "Memberikan +1 VP saat dirahasiakan.", false),
            new CardData("knight",   "⚔", "KARTU PENJAGA", "Penjaga",
                "Pindahkan Nimon Ungu, lalu curi 1 kartu sumber daya.", true)
        );
        boolean first = true;
        for (CardData c : cards) {
            VBox card = buildCard(c, first);
            if (first) selected = card;
            cardRow.getChildren().add(card);
            first = false;
        }
    }

    private VBox buildCard(CardData c, boolean isSelected) {
        VBox card = new VBox();
        card.getStyleClass().add("exp-card");
        if (isSelected) card.getStyleClass().add("is-selected");

        Label header = new Label(c.headerLabel());
        header.getStyleClass().addAll("exp-card-header", "exp-card-header-" + c.kind());
        header.setMaxWidth(Double.MAX_VALUE);

        VBox body = new VBox(8);
        body.setAlignment(Pos.CENTER);
        body.setStyle("-fx-padding: 12;");
        StackPane iconBox = new StackPane(new Label(c.icon()));
        iconBox.setMinSize(56, 56); iconBox.setMaxSize(56, 56);
        iconBox.setStyle("-fx-background-color: #fff8e1; -fx-background-radius: 12;"
            + " -fx-border-color: -parchment-line; -fx-border-radius: 12;");
        ((Label) iconBox.getChildren().get(0)).setStyle("-fx-font-size: 32px;");

        Label name = new Label(c.name());
        name.setStyle("-fx-font-weight: 800; -fx-font-size: 13px;");

        Label desc = new Label(c.desc());
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: -ink-mute;");
        desc.setAlignment(Pos.CENTER);
        desc.setMaxWidth(140);

        body.getChildren().addAll(iconBox, name, desc);

        Label footer = new Label(c.playable() ? "DAPAT DIMAINKAN" : "PASIF");
        footer.getStyleClass().add("exp-card-footer");
        if (!c.playable()) footer.getStyleClass().add("is-passive");
        footer.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(header, body, footer);
        card.setOnMouseClicked(e -> select(card));
        return card;
    }

    private void select(VBox card) {
        if (selected != null) selected.getStyleClass().remove("is-selected");
        selected = card;
        if (!card.getStyleClass().contains("is-selected")) {
            card.getStyleClass().add("is-selected");
        }
    }

    @FXML
    private void onPlay() {
        System.out.println("[Cards] play selected");
        close();
    }

    @FXML
    private void onClose() { close(); }

    private void close() {
        if (cardRow == null) return;
        Stage st = (Stage) cardRow.getScene().getWindow();
        st.close();
    }
}
