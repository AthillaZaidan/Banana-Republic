package com.bananarepublic.controller;

import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.Navigator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;

public class GameResultController {
    @FXML private Pane livingLayer;
    @FXML private TableView<ScoreRow> scoreTable;

    @FXML
    public void initialize() {
        LivingBackground.attach(livingLayer, LivingBackground.Variant.PARCHMENT);

        ObservableList<ScoreRow> data = FXCollections.observableArrayList(
            new ScoreRow("🏆 Gro",    4, 3, 1, 0, 11, true),
            new ScoreRow("Stewart",   3, 2, 1, 1, 9,  false),
            new ScoreRow("Kebin",     3, 1, 0, 2, 7,  false),
            new ScoreRow("Tara",      2, 1, 1, 1, 6,  false)
        );
        scoreTable.setItems(data);

        PseudoClass winnerClass = PseudoClass.getPseudoClass("winner");
        scoreTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ScoreRow item, boolean empty) {
                super.updateItem(item, empty);
                pseudoClassStateChanged(winnerClass, !empty && item != null && item.isWinner());
            }
        });
    }

    @FXML
    private void onViewBoard() {
        System.out.println("[GameResult] view final board");
        Navigator.goTo("/fxml/game.fxml");
    }

    @FXML
    private void onMainMenu() {
        Navigator.goTo("/fxml/main_menu.fxml");
    }

    public static class ScoreRow {
        private final String name;
        private final int pos, lab, spec, secret, total;
        private final boolean winner;

        public ScoreRow(String name, int pos, int lab, int spec, int secret, int total, boolean winner) {
            this.name = name; this.pos = pos; this.lab = lab;
            this.spec = spec; this.secret = secret; this.total = total;
            this.winner = winner;
        }
        public String getName()   { return name; }
        public int getPos()       { return pos; }
        public int getLab()       { return lab; }
        public int getSpec()      { return spec; }
        public int getSecret()    { return secret; }
        public int getTotal()     { return total; }
        public boolean isWinner() { return winner; }
    }
}
