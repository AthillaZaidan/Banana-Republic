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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class VictoryService {
    public static final int WINNING_POINTS = 10;
    private static final int LONGEST_ROAD_MINIMUM = 5;
    private static final int LARGEST_ARMY_MINIMUM = 3;

    public int calculateVictoryPoints(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        return calculatePublicVictoryPoints(player) + player.getSecretVictoryPoints();
    }

    public int calculatePublicVictoryPoints(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        int buildingPoints = player.getOwnedBuildings().stream()
                .mapToInt(Building::getVictoryPoint)
                .sum();

        int specialPoints = player.getSpecialCards().size() * 2;
        return buildingPoints + specialPoints;
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
        Map<Player, Integer> lengthsByPlayer = new LinkedHashMap<>();
        for (Player player : state.getPlayers()) {
            int length = calculateLongestRoad(player);
            lengthsByPlayer.put(player, length);
        }

        int bestLength = lengthsByPlayer.values().stream()
                .filter(length -> length >= LONGEST_ROAD_MINIMUM)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(LONGEST_ROAD_MINIMUM - 1);
        List<Player> leaders = lengthsByPlayer.entrySet().stream()
                .filter(entry -> entry.getValue() == bestLength)
                .map(Map.Entry::getKey)
                .toList();

        Player nextHolder;
        if (leaders.isEmpty()) {
            nextHolder = null;
        } else if (leaders.size() == 1) {
            nextHolder = leaders.getFirst();
        } else if (currentHolder != null && leaders.contains(currentHolder)) {
            nextHolder = currentHolder;
        } else {
            nextHolder = null;
        }

        if (nextHolder != currentHolder) {
            if (currentHolder != null) {
                currentHolder.removeSpecialCard(SpecialCardType.LONGEST_ROAD);
            }
            if (nextHolder != null) {
                nextHolder.addSpecialCard(SpecialCardType.LONGEST_ROAD);
            }
            state.setLongestRoadHolder(nextHolder);
        }
    }

    private int dfs(Intersection intersection, Player player, Set<Path> visitedPaths) {
        if (!visitedPaths.isEmpty() && isBlockedByOpponentBuilding(intersection, player)) {
            return 0;
        }

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

    private boolean isBlockedByOpponentBuilding(Intersection intersection, Player player) {
        return intersection.getBuilding()
                .map(building -> !building.isOwnedBy(player))
                .orElse(false);
    }
}
