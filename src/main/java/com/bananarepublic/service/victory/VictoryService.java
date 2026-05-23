package com.bananarepublic.service.victory;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.building.Building;
import com.bananarepublic.model.player.Player;

import java.util.Objects;
import java.util.Optional;

public class VictoryService {
    public static final int WINNING_POINTS = 10;

    public int calculateVictoryPoints(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        return player.getOwnedBuildings().stream()
                .mapToInt(Building::getVictoryPoint)
                .sum();
    }

    public Optional<Player> findWinner(GameState state) {
        Objects.requireNonNull(state, "Game state cannot be null");

        Player currentPlayer = state.getCurrentPlayer();
        if (calculateVictoryPoints(currentPlayer) >= WINNING_POINTS) {
            return Optional.of(currentPlayer);
        }

        return Optional.empty();
    }
}
