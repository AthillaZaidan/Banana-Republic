package com.bananarepublic.engine;

import com.bananarepublic.model.board.Board;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.Bank;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GameState {
    private final Board board;
    private final List<Player> players;
    private final Bank bank;
    private final TurnState turnState;
    private String nimonTileId;
    private Player winner;

    public GameState(Board board, List<Player> players, Bank bank, TurnState turnState) {
        this.board = Objects.requireNonNull(board, "Board cannot be null");
        this.players = List.copyOf(Objects.requireNonNull(players, "Players cannot be null"));
        this.bank = Objects.requireNonNull(bank, "Bank cannot be null");
        this.turnState = Objects.requireNonNull(turnState, "Turn state cannot be null");
        this.nimonTileId = findDesertTileId(board);
    }

    public Board getBoard() {
        return board;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Bank getBank() {
        return bank;
    }

    public TurnState getTurnState() {
        return turnState;
    }

    public Player getCurrentPlayer() {
        return players.get(turnState.getCurrentPlayerIndex());
    }

    public Optional<Player> getWinner() {
        return Optional.ofNullable(winner);
    }

    public void setWinner(Player winner) {
        this.winner = winner;
        if (winner != null) {
            turnState.setPhase(TurnPhase.GAME_OVER);
        }
    }

    public boolean isGameOver() {
        return winner != null;
    }

    public String getNimonTileId() {
        return nimonTileId;
    }

    public void moveNimonTo(String tileId) {
        if (tileId == null || tileId.isBlank()) {
            throw new IllegalArgumentException("Nimon tile id cannot be empty");
        }

        if (tileId.equals(nimonTileId)) {
            throw new IllegalArgumentException("Nimon must move to a different tile");
        }

        if (board.getTile(tileId) == null) {
            throw new IllegalArgumentException("Unknown tile id: " + tileId);
        }

        this.nimonTileId = tileId;
    }

    public Player getPlayerById(String playerId) {
        return players.stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown player id: " + playerId));
    }

    private String findDesertTileId(Board board) {
        return board.getTiles().stream()
                .filter(tile -> !tile.getTerrainType().producesResource())
                .map(HexTile::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Board must contain a desert tile"));
    }
}
