package com.bananarepublic.service.victory;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.building.Building;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.player.SpecialCardType;
import com.bananarepublic.model.transport.Pipe;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class VictoryService {
    public static final int WINNING_POINTS = 10;
    private static final int LONGEST_ROAD_MINIMUM = 5;
    private static final int LARGEST_ARMY_MINIMUM = 3;

    public int calculateVictoryPoints(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        int buildingPoints = player.getOwnedBuildings().stream()
                .mapToInt(Building::getVictoryPoint)
                .sum();

        int specialPoints = player.getSpecialCards().size() * 2;
        return buildingPoints + player.getSecretVictoryPoints() + specialPoints;
    }

    public Optional<Player> findWinner(GameState state) {
        Objects.requireNonNull(state, "Game state cannot be null");

        Player currentPlayer = state.getCurrentPlayer();
        if (calculateVictoryPoints(currentPlayer) >= WINNING_POINTS) {
            return Optional.of(currentPlayer);
        }

        return Optional.empty();
    }

    public void updateSpecialCards(GameState state) {
        Objects.requireNonNull(state, "Game state cannot be null");
        updateLargestArmy(state);
        updateLongestRoad(state);
    }

    public int calculateLongestRoad(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        List<Path> paths = player.getOwnedPipes().stream()
                .map(Pipe::getLocation)
                .toList();

        int longest = 0;
        for (Path path : paths) {
            longest = Math.max(longest, dfs(path.getEndpointA(), player, new HashSet<>()));
            longest = Math.max(longest, dfs(path.getEndpointB(), player, new HashSet<>()));
        }

        return longest;
    }

    private void updateLargestArmy(GameState state) {
        Player currentHolder = state.getLargestArmyHolder().orElse(null);
        Player bestPlayer = currentHolder;
        int bestCount = currentHolder == null ? LARGEST_ARMY_MINIMUM - 1 : currentHolder.getPlayedKnightCount();

        for (Player player : state.getPlayers()) {
            if (player.getPlayedKnightCount() >= LARGEST_ARMY_MINIMUM
                    && player.getPlayedKnightCount() > bestCount) {
                bestPlayer = player;
                bestCount = player.getPlayedKnightCount();
            }
        }

        if (bestPlayer != currentHolder) {
            if (currentHolder != null) {
                currentHolder.removeSpecialCard(SpecialCardType.LARGEST_ARMY);
            }
            if (bestPlayer != null) {
                bestPlayer.addSpecialCard(SpecialCardType.LARGEST_ARMY);
            }
            state.setLargestArmyHolder(bestPlayer);
        }
    }

    private void updateLongestRoad(GameState state) {
        Player currentHolder = state.getLongestRoadHolder().orElse(null);
        Player bestPlayer = currentHolder;
        int bestLength = currentHolder == null ? LONGEST_ROAD_MINIMUM - 1 : calculateLongestRoad(currentHolder);
        boolean tiedCurrentHolder = false;

        for (Player player : state.getPlayers()) {
            int length = calculateLongestRoad(player);
            if (length >= LONGEST_ROAD_MINIMUM && length > bestLength) {
                bestPlayer = player;
                bestLength = length;
                tiedCurrentHolder = false;
            } else if (currentHolder != null && player != currentHolder && length == bestLength) {
                tiedCurrentHolder = true;
            }
        }

        if (currentHolder != null && calculateLongestRoad(currentHolder) < LONGEST_ROAD_MINIMUM) {
            currentHolder.removeSpecialCard(SpecialCardType.LONGEST_ROAD);
            state.setLongestRoadHolder(null);
            return;
        }

        if (bestPlayer != currentHolder && !tiedCurrentHolder) {
            if (currentHolder != null) {
                currentHolder.removeSpecialCard(SpecialCardType.LONGEST_ROAD);
            }
            if (bestPlayer != null) {
                bestPlayer.addSpecialCard(SpecialCardType.LONGEST_ROAD);
            }
            state.setLongestRoadHolder(bestPlayer);
        }
    }

    private int dfs(Intersection intersection, Player player, Set<Path> visitedPaths) {
        int best = 0;

        for (Path path : intersection.getConnectedPaths()) {
            if (visitedPaths.contains(path) || path.getPipe().isEmpty() || !path.getPipe().orElseThrow().isOwnedBy(player)) {
                continue;
            }

            visitedPaths.add(path);
            int length = 1 + dfs(path.getOtherEndpoint(intersection), player, visitedPaths);
            best = Math.max(best, length);
            visitedPaths.remove(path);
        }

        return best;
    }
}
