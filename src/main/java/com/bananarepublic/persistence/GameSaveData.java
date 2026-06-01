package com.bananarepublic.persistence;

import com.bananarepublic.engine.GameEngine;
import com.bananarepublic.engine.GameState;
import com.bananarepublic.engine.TurnPhase;
import com.bananarepublic.engine.TurnState;
import com.bananarepublic.exception.SaveLoadException;
import com.bananarepublic.model.board.Board;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.board.TerrainType;
import com.bananarepublic.model.building.Building;
import com.bananarepublic.model.building.BuildingType;
import com.bananarepublic.model.building.Laboratory;
import com.bananarepublic.model.building.MonitoringPost;
import com.bananarepublic.model.card.DevelopmentCard;
import com.bananarepublic.model.card.DevelopmentDeck;
import com.bananarepublic.model.card.KnightCard;
import com.bananarepublic.model.card.MonopolyCard;
import com.bananarepublic.model.card.RoadBuildingCard;
import com.bananarepublic.model.card.VictoryPointCard;
import com.bananarepublic.model.harbor.BananaHarbor;
import com.bananarepublic.model.harbor.BrickHarbor;
import com.bananarepublic.model.harbor.GenericHarbor;
import com.bananarepublic.model.harbor.Harbor;
import com.bananarepublic.model.harbor.OreHarbor;
import com.bananarepublic.model.harbor.WheatHarbor;
import com.bananarepublic.model.harbor.WoodHarbor;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.player.PlayerColor;
import com.bananarepublic.model.player.PlayerSupply;
import com.bananarepublic.model.player.SpecialCardType;
import com.bananarepublic.model.resource.Bank;
import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.model.transport.Pipe;
import com.bananarepublic.plugin.PluginExperimentCardAdapter;
import com.bananarepublic.plugin.PluginLoader;

import java.io.Serializable;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record GameSaveData(
        int version,
        String timestamp,
        BoardSaveData mapConfig,
        List<PlayerSaveData> players,
        int currentPlayerIndex,
        InventorySaveData bank,
        DeckSaveData deck,
        String robberPosition,
        TurnSaveData turn,
        String winnerPlayerId,
        String longestRoadHolderId,
        String largestArmyHolderId,
        boolean manualDiceEnabled,
        String botPluginJarPath,
        Set<String> botPlayerIds
) implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int CURRENT_VERSION = 2;

    public GameSaveData {
        if (version != CURRENT_VERSION) {
            throw new SaveLoadException("Unsupported save version: " + version);
        }
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        Objects.requireNonNull(mapConfig, "Map config cannot be null");
        players = List.copyOf(Objects.requireNonNull(players, "Players cannot be null"));
        Objects.requireNonNull(bank, "Bank cannot be null");
        Objects.requireNonNull(deck, "Deck cannot be null");
        Objects.requireNonNull(robberPosition, "Robber position cannot be null");
        Objects.requireNonNull(turn, "Turn data cannot be null");
        botPlayerIds = Set.copyOf(Objects.requireNonNull(botPlayerIds, "Bot player ids cannot be null"));
    }

    public static GameSaveData fromState(GameState state) {
        Objects.requireNonNull(state, "Game state cannot be null");

        GameSaveData saveData = new GameSaveData(
                CURRENT_VERSION,
                Instant.now().toString(),
                BoardSaveData.fromBoard(state.getBoard()),
                state.getPlayers().stream()
                        .map(PlayerSaveData::fromPlayer)
                        .toList(),
                state.getTurnState().getCurrentPlayerIndex(),
                InventorySaveData.fromInventory(state.getBank().getInventoryCopy()),
                DeckSaveData.fromDeck(requireDeck(state)),
                state.getNimonTileId(),
                TurnSaveData.fromTurnState(state.getTurnState()),
                state.getWinner().map(Player::getId).orElse(null),
                state.getLongestRoadHolder().map(Player::getId).orElse(null),
                state.getLargestArmyHolder().map(Player::getId).orElse(null),
                true,
                null,
                Set.of()
        );

        assert saveData.players.size() == state.getPlayers().size();
        return saveData;
    }

    public static GameSaveData fromEngine(GameEngine engine) {
        Objects.requireNonNull(engine, "Game engine cannot be null");
        GameState state = engine.getState();

        GameSaveData saveData = new GameSaveData(
                CURRENT_VERSION,
                Instant.now().toString(),
                BoardSaveData.fromBoard(state.getBoard()),
                state.getPlayers().stream()
                        .map(PlayerSaveData::fromPlayer)
                        .toList(),
                state.getTurnState().getCurrentPlayerIndex(),
                InventorySaveData.fromInventory(state.getBank().getInventoryCopy()),
                DeckSaveData.fromDeck(requireDeck(state)),
                state.getNimonTileId(),
                TurnSaveData.fromTurnState(state.getTurnState()),
                state.getWinner().map(Player::getId).orElse(null),
                state.getLongestRoadHolder().map(Player::getId).orElse(null),
                state.getLargestArmyHolder().map(Player::getId).orElse(null),
                engine.isManualDiceEnabled(),
                engine.getActiveBotPluginJarPath(),
                engine.getBotPlayerIds()
        );

        assert saveData.players.size() == state.getPlayers().size();
        return saveData;
    }

    public GameState toGameState() {
        Board board = mapConfig.toBoard();
        List<Player> restoredPlayers = restorePlayers();
        Map<String, Player> playersById = indexPlayers(restoredPlayers);
        restoreStructures(board, playersById);

        TurnState turnState = turn.toTurnState(currentPlayerIndex);
        Bank restoredBank = bank.toBank();
        GameState restoredState = new GameState(board, restoredPlayers, restoredBank, turnState);

        if (!robberPosition.equals(restoredState.getNimonTileId())) {
            restoredState.moveNimonTo(robberPosition);
        }

        restoredState.setDevelopmentDeck(deck.toDeck());
        restoredState.setLongestRoadHolder(resolvePlayer(playersById, longestRoadHolderId));
        restoredState.setLargestArmyHolder(resolvePlayer(playersById, largestArmyHolderId));

        Player winner = resolvePlayer(playersById, winnerPlayerId);
        if (winner != null) {
            restoredState.setWinner(winner);
        }

        assert restoredState.getBoard().getTiles().size() == mapConfig.tiles().size();
        return restoredState;
    }

    public Map<String, Object> toSummaryMap() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("version", version);
        summary.put("timestamp", timestamp);
        summary.put("mapConfig", mapConfig.toSummaryMap());
        summary.put("players", players.stream().map(PlayerSaveData::toSummaryMap).toList());
        summary.put("currentPlayerIndex", currentPlayerIndex);
        summary.put("bank", bank.toSummaryMap());
        summary.put("deck", deck.toSummaryMap());
        summary.put("robberPosition", robberPosition);
        summary.put("turn", turn.toSummaryMap());
        summary.put("winnerPlayerId", winnerPlayerId);
        summary.put("longestRoadHolderId", longestRoadHolderId);
        summary.put("largestArmyHolderId", largestArmyHolderId);
        summary.put("manualDiceEnabled", manualDiceEnabled);
        summary.put("botPluginJarPath", botPluginJarPath);
        summary.put("botPlayerIds", botPlayerIds.stream().sorted().toList());
        return summary;
    }

    private static DevelopmentDeck requireDeck(GameState state) {
        DevelopmentDeck deck = state.getDevelopmentDeck();
        if (deck == null) {
            throw new SaveLoadException("Game state does not contain a development deck");
        }
        return deck;
    }

    private List<Player> restorePlayers() {
        List<Player> restoredPlayers = new ArrayList<>();

        for (PlayerSaveData playerSave : players) {
            Player player = new Player(playerSave.id(), playerSave.name(), playerSave.color());
            playerSave.resources().applyTo(player);
            playerSave.supply().applyTo(player.getSupply());
            player.addSecretVictoryPoints(playerSave.secretVictoryPoints());

            for (int i = 0; i < playerSave.playedKnightCount(); i++) {
                player.incrementPlayedKnightCount();
            }

            for (SpecialCardType specialCard : playerSave.specialCards()) {
                player.addSpecialCard(specialCard);
            }

            for (CardSaveData handCard : playerSave.handCards()) {
                player.addCard(handCard.toCard());
            }

            restoredPlayers.add(player);
        }

        assert restoredPlayers.size() == players.size();
        return restoredPlayers;
    }

    private void restoreStructures(Board board, Map<String, Player> playersById) {
        for (IntersectionSaveData intersectionSave : mapConfig.intersections()) {
            if (intersectionSave.building() == null) {
                continue;
            }

            Intersection intersection = board.getIntersection(intersectionSave.id());
            BuildingSaveData buildingSave = intersectionSave.building();
            Player owner = requirePlayer(playersById, buildingSave.ownerPlayerId());
            Building building = switch (buildingSave.buildingType()) {
                case MONITORING_POST -> new MonitoringPost(owner, intersection);
                case LABORATORY -> new Laboratory(owner, intersection);
            };

            intersection.placeBuilding(building);
            owner.registerBuilding(building);
        }

        for (PathSaveData pathSave : mapConfig.paths()) {
            if (pathSave.pipe() == null) {
                continue;
            }

            Path path = board.getPath(pathSave.id());
            PipeSaveData pipeSave = pathSave.pipe();
            Player owner = requirePlayer(playersById, pipeSave.ownerPlayerId());
            Pipe pipe = new Pipe(pipeSave.id(), owner, path);
            path.placePipe(pipe);
            owner.registerPipe(pipe);
        }

        for (PlayerSaveData playerSave : players) {
            Player player = requirePlayer(playersById, playerSave.id());
            if (player.getOwnedBuildings().size() != playerSave.ownedBuildings().size()) {
                throw new SaveLoadException("Building ownership mismatch for player " + playerSave.id());
            }
            if (player.getOwnedPipes().size() != playerSave.ownedPipePathIds().size()) {
                throw new SaveLoadException("Pipe ownership mismatch for player " + playerSave.id());
            }
        }
    }

    private static Map<String, Player> indexPlayers(List<Player> players) {
        Map<String, Player> indexedPlayers = new LinkedHashMap<>();

        for (Player player : players) {
            indexedPlayers.put(player.getId(), player);
        }

        return indexedPlayers;
    }

    private static Player requirePlayer(Map<String, Player> playersById, String playerId) {
        Player player = playersById.get(playerId);
        if (player == null) {
            throw new SaveLoadException("Unknown player referenced in save: " + playerId);
        }
        return player;
    }

    private static Player resolvePlayer(Map<String, Player> playersById, String playerId) {
        if (playerId == null) {
            return null;
        }
        return requirePlayer(playersById, playerId);
    }

    public record BoardSaveData(
            List<TileSaveData> tiles,
            List<IntersectionSaveData> intersections,
            List<PathSaveData> paths,
            List<HarborSaveData> harbors
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public BoardSaveData {
            tiles = List.copyOf(Objects.requireNonNull(tiles, "Tiles cannot be null"));
            intersections = List.copyOf(Objects.requireNonNull(intersections, "Intersections cannot be null"));
            paths = List.copyOf(Objects.requireNonNull(paths, "Paths cannot be null"));
            harbors = List.copyOf(Objects.requireNonNull(harbors, "Harbors cannot be null"));
        }

        static BoardSaveData fromBoard(Board board) {
            Comparator<HexTile> tileComparator = Comparator.comparing(HexTile::getId);
            Comparator<Intersection> intersectionComparator = Comparator.comparing(Intersection::getId);
            Comparator<Path> pathComparator = Comparator.comparing(Path::getId);
            Comparator<Harbor> harborComparator = Comparator.comparing(Harbor::getId);

            return new BoardSaveData(
                    board.getTiles().stream()
                            .sorted(tileComparator)
                            .map(TileSaveData::fromTile)
                            .toList(),
                    board.getIntersections().stream()
                            .sorted(intersectionComparator)
                            .map(IntersectionSaveData::fromIntersection)
                            .toList(),
                    board.getPaths().stream()
                            .sorted(pathComparator)
                            .map(PathSaveData::fromPath)
                            .toList(),
                    board.getHarbors().stream()
                            .sorted(harborComparator)
                            .map(HarborSaveData::fromHarbor)
                            .toList()
            );
        }

        Board toBoard() {
            Map<String, HexTile> tilesById = new LinkedHashMap<>();
            Map<String, Intersection> intersectionsById = new LinkedHashMap<>();
            Map<String, Path> pathsById = new LinkedHashMap<>();
            Map<String, Harbor> harborsById = new LinkedHashMap<>();

            for (TileSaveData tileSave : tiles) {
                tilesById.put(tileSave.id(), new HexTile(tileSave.id(), tileSave.terrainType(), tileSave.token()));
            }

            for (IntersectionSaveData intersectionSave : intersections) {
                intersectionsById.put(intersectionSave.id(), new Intersection(intersectionSave.id()));
            }

            for (PathSaveData pathSave : paths) {
                Intersection endpointA = intersectionsById.get(pathSave.endpointAId());
                Intersection endpointB = intersectionsById.get(pathSave.endpointBId());
                if (endpointA == null || endpointB == null) {
                    throw new SaveLoadException("Path references unknown intersection: " + pathSave.id());
                }

                Path path = new Path(pathSave.id(), endpointA, endpointB);
                endpointA.addConnectedPath(path);
                endpointB.addConnectedPath(path);
                pathsById.put(pathSave.id(), path);
            }

            for (TileSaveData tileSave : tiles) {
                HexTile tile = tilesById.get(tileSave.id());
                for (String intersectionId : tileSave.intersectionIds()) {
                    Intersection intersection = intersectionsById.get(intersectionId);
                    if (intersection == null) {
                        throw new SaveLoadException("Tile references unknown intersection: " + intersectionId);
                    }
                    tile.addIntersection(intersection);
                    intersection.addAdjacentTile(tile);
                }
            }

            for (PathSaveData pathSave : paths) {
                Path path = pathsById.get(pathSave.id());
                for (String tileId : pathSave.adjacentTileIds()) {
                    HexTile tile = tilesById.get(tileId);
                    if (tile == null) {
                        throw new SaveLoadException("Path references unknown tile: " + tileId);
                    }
                    path.addAdjacentTile(tile);
                }
            }

            for (HarborSaveData harborSave : harbors) {
                Path attachedPath = pathsById.get(harborSave.pathId());
                if (attachedPath == null) {
                    throw new SaveLoadException("Harbor references unknown path: " + harborSave.pathId());
                }

                Harbor harbor = harborSave.toHarbor(attachedPath);
                attachedPath.attachHarbor(harbor);
                harborsById.put(harbor.getId(), harbor);
            }

            return new Board(tilesById, intersectionsById, pathsById, harborsById);
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("tiles", tiles.stream().map(TileSaveData::toSummaryMap).toList());
            summary.put("intersections", intersections.stream().map(IntersectionSaveData::toSummaryMap).toList());
            summary.put("paths", paths.stream().map(PathSaveData::toSummaryMap).toList());
            summary.put("harbors", harbors.stream().map(HarborSaveData::toSummaryMap).toList());
            return summary;
        }
    }

    public record TileSaveData(
            String id,
            TerrainType terrainType,
            Integer token,
            List<String> intersectionIds
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public TileSaveData {
            Objects.requireNonNull(id, "Tile id cannot be null");
            Objects.requireNonNull(terrainType, "Terrain type cannot be null");
            intersectionIds = List.copyOf(Objects.requireNonNull(intersectionIds, "Intersection ids cannot be null"));
        }

        static TileSaveData fromTile(HexTile tile) {
            return new TileSaveData(
                    tile.getId(),
                    tile.getTerrainType(),
                    tile.getToken(),
                    tile.getIntersections().stream()
                            .map(Intersection::getId)
                            .toList()
            );
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", id);
            summary.put("terrainType", terrainType);
            summary.put("token", token);
            summary.put("intersectionIds", intersectionIds);
            return summary;
        }
    }

    public record IntersectionSaveData(
            String id,
            List<String> connectedPathIds,
            List<String> adjacentTileIds,
            BuildingSaveData building
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public IntersectionSaveData {
            Objects.requireNonNull(id, "Intersection id cannot be null");
            connectedPathIds = List.copyOf(Objects.requireNonNull(connectedPathIds, "Connected path ids cannot be null"));
            adjacentTileIds = List.copyOf(Objects.requireNonNull(adjacentTileIds, "Adjacent tile ids cannot be null"));
        }

        static IntersectionSaveData fromIntersection(Intersection intersection) {
            return new IntersectionSaveData(
                    intersection.getId(),
                    intersection.getConnectedPaths().stream()
                            .map(Path::getId)
                            .toList(),
                    intersection.getAdjacentTiles().stream()
                            .map(HexTile::getId)
                            .toList(),
                    intersection.getBuilding()
                            .map(BuildingSaveData::fromBuilding)
                            .orElse(null)
            );
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", id);
            summary.put("connectedPathIds", connectedPathIds);
            summary.put("adjacentTileIds", adjacentTileIds);
            summary.put("building", building == null ? null : building.toSummaryMap());
            return summary;
        }
    }

    public record PathSaveData(
            String id,
            String endpointAId,
            String endpointBId,
            List<String> adjacentTileIds,
            PipeSaveData pipe
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public PathSaveData {
            Objects.requireNonNull(id, "Path id cannot be null");
            Objects.requireNonNull(endpointAId, "Endpoint A id cannot be null");
            Objects.requireNonNull(endpointBId, "Endpoint B id cannot be null");
            adjacentTileIds = List.copyOf(Objects.requireNonNull(adjacentTileIds, "Adjacent tile ids cannot be null"));
        }

        static PathSaveData fromPath(Path path) {
            return new PathSaveData(
                    path.getId(),
                    path.getEndpointA().getId(),
                    path.getEndpointB().getId(),
                    path.getAdjacentTiles().stream()
                            .map(HexTile::getId)
                            .toList(),
                    path.getPipe()
                            .map(PipeSaveData::fromPipe)
                            .orElse(null)
            );
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", id);
            summary.put("endpointAId", endpointAId);
            summary.put("endpointBId", endpointBId);
            summary.put("adjacentTileIds", adjacentTileIds);
            summary.put("pipe", pipe == null ? null : pipe.toSummaryMap());
            return summary;
        }
    }

    public record BuildingSaveData(
            String ownerPlayerId,
            BuildingType buildingType
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public BuildingSaveData {
            Objects.requireNonNull(ownerPlayerId, "Owner player id cannot be null");
            Objects.requireNonNull(buildingType, "Building type cannot be null");
        }

        static BuildingSaveData fromBuilding(Building building) {
            return new BuildingSaveData(building.getOwner().getId(), building.getType());
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("ownerPlayerId", ownerPlayerId);
            summary.put("buildingType", buildingType);
            return summary;
        }
    }

    public record PipeSaveData(
            String id,
            String ownerPlayerId
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public PipeSaveData {
            Objects.requireNonNull(id, "Pipe id cannot be null");
            Objects.requireNonNull(ownerPlayerId, "Owner player id cannot be null");
        }

        static PipeSaveData fromPipe(Pipe pipe) {
            return new PipeSaveData(pipe.getId(), pipe.getOwner().getId());
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", id);
            summary.put("ownerPlayerId", ownerPlayerId);
            return summary;
        }
    }

    public enum HarborKind {
        GENERIC,
        WOOD,
        BRICK,
        WHEAT,
        ORE,
        BANANA
    }

    public record HarborSaveData(
            String id,
            HarborKind harborKind,
            String pathId
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public HarborSaveData {
            Objects.requireNonNull(id, "Harbor id cannot be null");
            Objects.requireNonNull(harborKind, "Harbor kind cannot be null");
            Objects.requireNonNull(pathId, "Path id cannot be null");
        }

        static HarborSaveData fromHarbor(Harbor harbor) {
            HarborKind kind = switch (harbor) {
                case GenericHarbor ignored -> HarborKind.GENERIC;
                case WoodHarbor ignored -> HarborKind.WOOD;
                case BrickHarbor ignored -> HarborKind.BRICK;
                case WheatHarbor ignored -> HarborKind.WHEAT;
                case OreHarbor ignored -> HarborKind.ORE;
                case BananaHarbor ignored -> HarborKind.BANANA;
                default -> throw new SaveLoadException("Unsupported harbor type: " + harbor.getClass().getName());
            };

            return new HarborSaveData(harbor.getId(), kind, harbor.getAttachedPath().getId());
        }

        Harbor toHarbor(Path attachedPath) {
            return switch (harborKind) {
                case GENERIC -> new GenericHarbor(id, attachedPath);
                case WOOD -> new WoodHarbor(id, attachedPath);
                case BRICK -> new BrickHarbor(id, attachedPath);
                case WHEAT -> new WheatHarbor(id, attachedPath);
                case ORE -> new OreHarbor(id, attachedPath);
                case BANANA -> new BananaHarbor(id, attachedPath);
            };
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", id);
            summary.put("harborKind", harborKind);
            summary.put("pathId", pathId);
            return summary;
        }
    }

    public record PlayerSaveData(
            String id,
            String name,
            PlayerColor color,
            InventorySaveData resources,
            PlayerSupplySaveData supply,
            List<OwnedBuildingSaveData> ownedBuildings,
            List<String> ownedPipePathIds,
            List<CardSaveData> handCards,
            Set<SpecialCardType> specialCards,
            int playedKnightCount,
            int secretVictoryPoints
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public PlayerSaveData {
            Objects.requireNonNull(id, "Player id cannot be null");
            Objects.requireNonNull(name, "Player name cannot be null");
            Objects.requireNonNull(color, "Player color cannot be null");
            Objects.requireNonNull(resources, "Resources cannot be null");
            Objects.requireNonNull(supply, "Supply cannot be null");
            ownedBuildings = List.copyOf(Objects.requireNonNull(ownedBuildings, "Owned buildings cannot be null"));
            ownedPipePathIds = List.copyOf(Objects.requireNonNull(ownedPipePathIds, "Owned pipe path ids cannot be null"));
            handCards = List.copyOf(Objects.requireNonNull(handCards, "Hand cards cannot be null"));
            specialCards = Set.copyOf(Objects.requireNonNull(specialCards, "Special cards cannot be null"));
        }

        static PlayerSaveData fromPlayer(Player player) {
            return new PlayerSaveData(
                    player.getId(),
                    player.getName(),
                    player.getColor(),
                    InventorySaveData.fromInventory(player.getResourceInventoryCopy()),
                    PlayerSupplySaveData.fromSupply(player.getSupply()),
                    player.getOwnedBuildings().stream()
                            .sorted(Comparator.comparing(building -> building.getLocation().getId()))
                            .map(building -> new OwnedBuildingSaveData(building.getLocation().getId(), building.getType()))
                            .toList(),
                    player.getOwnedPipes().stream()
                            .map(pipe -> pipe.getLocation().getId())
                            .sorted()
                            .toList(),
                    player.getHandCards().stream()
                            .map(CardSaveData::fromCard)
                            .toList(),
                    player.getSpecialCards(),
                    player.getPlayedKnightCount(),
                    player.getSecretVictoryPoints()
            );
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", id);
            summary.put("name", name);
            summary.put("color", color);
            summary.put("resources", resources.toSummaryMap());
            summary.put("supply", supply.toSummaryMap());
            summary.put("ownedBuildings", ownedBuildings.stream().map(OwnedBuildingSaveData::toSummaryMap).toList());
            summary.put("ownedPipePathIds", ownedPipePathIds);
            summary.put("handCards", handCards.stream().map(CardSaveData::toSummaryMap).toList());
            summary.put("specialCards", specialCards.stream().map(Enum::name).sorted().toList());
            summary.put("playedKnightCount", playedKnightCount);
            summary.put("secretVictoryPoints", secretVictoryPoints);
            return summary;
        }
    }

    public record OwnedBuildingSaveData(
            String intersectionId,
            BuildingType buildingType
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public OwnedBuildingSaveData {
            Objects.requireNonNull(intersectionId, "Intersection id cannot be null");
            Objects.requireNonNull(buildingType, "Building type cannot be null");
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("intersectionId", intersectionId);
            summary.put("buildingType", buildingType);
            return summary;
        }
    }

    public record PlayerSupplySaveData(
            int remainingMonitoringPosts,
            int remainingLaboratories,
            int remainingPipes
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        static PlayerSupplySaveData fromSupply(PlayerSupply supply) {
            return new PlayerSupplySaveData(
                    supply.getRemainingMonitoringPosts(),
                    supply.getRemainingLaboratories(),
                    supply.getRemainingPipes()
            );
        }

        void applyTo(PlayerSupply supply) {
            if (remainingMonitoringPosts < 0
                    || remainingLaboratories < 0
                    || remainingPipes < 0
                    || remainingMonitoringPosts > PlayerSupply.INITIAL_MONITORING_POSTS
                    || remainingLaboratories > PlayerSupply.INITIAL_LABORATORIES
                    || remainingPipes > PlayerSupply.INITIAL_PIPES) {
                throw new SaveLoadException("Invalid player supply values in save data");
            }

            while (supply.getRemainingMonitoringPosts() > remainingMonitoringPosts) {
                supply.useMonitoringPost();
            }
            while (supply.getRemainingLaboratories() > remainingLaboratories) {
                supply.useLaboratory();
            }
            while (supply.getRemainingPipes() > remainingPipes) {
                supply.usePipe();
            }
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("remainingMonitoringPosts", remainingMonitoringPosts);
            summary.put("remainingLaboratories", remainingLaboratories);
            summary.put("remainingPipes", remainingPipes);
            return summary;
        }
    }

    public record InventorySaveData(
            Map<ResourceType, Integer> amounts
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public InventorySaveData {
            Map<ResourceType, Integer> copiedAmounts = new EnumMap<>(ResourceType.class);
            copiedAmounts.putAll(Objects.requireNonNull(amounts, "Inventory amounts cannot be null"));
            for (ResourceType type : ResourceType.values()) {
                copiedAmounts.putIfAbsent(type, 0);
                Integer amount = copiedAmounts.get(type);
                if (amount == null || amount < 0) {
                    throw new SaveLoadException("Invalid inventory amount for " + type);
                }
            }
            amounts = Map.copyOf(copiedAmounts);
        }

        static InventorySaveData fromInventory(ResourceInventory inventory) {
            return new InventorySaveData(inventory.asMap());
        }

        Bank toBank() {
            Bank bank = new Bank();
            for (ResourceType type : ResourceType.values()) {
                int target = amounts.get(type);
                int current = bank.getAmount(type);
                if (target < current) {
                    bank.take(type, current - target);
                } else if (target > current) {
                    bank.returnResource(type, target - current);
                }
            }
            return bank;
        }

        void applyTo(Player player) {
            for (ResourceType type : ResourceType.values()) {
                int amount = amounts.get(type);
                if (amount > 0) {
                    player.addResource(type, amount);
                }
            }
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            for (ResourceType type : ResourceType.values()) {
                summary.put(type.name(), amounts.get(type));
            }
            return summary;
        }
    }

    public record DeckSaveData(
            List<CardSaveData> drawPile,
            List<CardSaveData> discardPile
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public DeckSaveData {
            drawPile = List.copyOf(Objects.requireNonNull(drawPile, "Draw pile cannot be null"));
            discardPile = List.copyOf(Objects.requireNonNull(discardPile, "Discard pile cannot be null"));
        }

        static DeckSaveData fromDeck(DevelopmentDeck deck) {
            return new DeckSaveData(
                    deck.getDrawPile().stream().map(CardSaveData::fromCard).toList(),
                    deck.getDiscardPile().stream().map(CardSaveData::fromCard).toList()
            );
        }

        DevelopmentDeck toDeck() {
            DevelopmentDeck restoredDeck = new DevelopmentDeck(
                    drawPile.stream()
                            .map(CardSaveData::toCard)
                            .toList()
            );

            for (CardSaveData discardedCard : discardPile) {
                restoredDeck.discard(discardedCard.toCard());
            }

            assert restoredDeck.size() == drawPile.size();
            return restoredDeck;
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("drawPileCount", drawPile.size());
            summary.put("discardPileCount", discardPile.size());
            return summary;
        }
    }

    public enum CardKind {
        KNIGHT,
        ROAD_BUILDING,
        MONOPOLY,
        VICTORY_POINT,
        PLUGIN
    }

    public record CardSaveData(
            String id,
            CardKind cardKind,
            boolean consumed,
            String pluginJarPath,
            String pluginClassName,
            String cardName,
            String description,
            boolean hidden
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public CardSaveData {
            Objects.requireNonNull(id, "Card id cannot be null");
            Objects.requireNonNull(cardKind, "Card kind cannot be null");
            Objects.requireNonNull(cardName, "Card name cannot be null");
            Objects.requireNonNull(description, "Card description cannot be null");
        }

        static CardSaveData fromCard(DevelopmentCard card) {
            if (card instanceof PluginExperimentCardAdapter pluginCard) {
                return new CardSaveData(
                        pluginCard.getId(),
                        CardKind.PLUGIN,
                        false,
                        pluginCard.getSourceJarPath(),
                        pluginCard.getImplementationClassName(),
                        pluginCard.getName(),
                        pluginCard.getDescription(),
                        pluginCard.isHidden()
                );
            }

            CardKind cardKind = switch (card) {
                case KnightCard ignored -> CardKind.KNIGHT;
                case RoadBuildingCard ignored -> CardKind.ROAD_BUILDING;
                case MonopolyCard ignored -> CardKind.MONOPOLY;
                case VictoryPointCard ignored -> CardKind.VICTORY_POINT;
                default -> throw new SaveLoadException("Unsupported development card type: " + card.getClass().getName());
            };

            boolean consumed = card instanceof VictoryPointCard victoryPointCard && victoryPointCard.isConsumed();
            return new CardSaveData(
                    card.getId(),
                    cardKind,
                    consumed,
                    null,
                    null,
                    card.getName(),
                    card.getDescription(),
                    card.isHidden()
            );
        }

        DevelopmentCard toCard() {
            return switch (cardKind) {
                case KNIGHT -> new KnightCard(id);
                case ROAD_BUILDING -> new RoadBuildingCard(id);
                case MONOPOLY -> new MonopolyCard(id);
                case VICTORY_POINT -> new VictoryPointCard(id, consumed);
                case PLUGIN -> restorePluginCard();
            };
        }

        private DevelopmentCard restorePluginCard() {
            if (pluginJarPath == null || pluginJarPath.isBlank() || pluginClassName == null || pluginClassName.isBlank()) {
                throw new SaveLoadException("Plugin card metadata is incomplete for card " + id);
            }

            return new PluginLoader().loadSingleCardFromJar(new File(pluginJarPath), pluginClassName, id);
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", id);
            summary.put("cardKind", cardKind);
            summary.put("cardName", cardName);
            summary.put("description", description);
            summary.put("hidden", hidden);
            if (cardKind == CardKind.VICTORY_POINT) {
                summary.put("consumed", consumed);
            }
            if (cardKind == CardKind.PLUGIN) {
                summary.put("pluginJarPath", pluginJarPath);
                summary.put("pluginClassName", pluginClassName);
            }
            return summary;
        }
    }

    public record TurnSaveData(
            TurnPhase phase,
            int setupRound,
            boolean hasRolledDice,
            boolean hasPlayedDevelopmentCard,
            int remainingSeconds,
            boolean waitingForSetupPipe,
            String setupPostIntersectionId,
            Set<String> newlyBoughtCardIds,
            Set<String> pendingDiscardPlayerIds,
            boolean nimonMovedThisSeven
    ) implements Serializable {
        private static final long serialVersionUID = 1L;

        public TurnSaveData {
            Objects.requireNonNull(phase, "Turn phase cannot be null");
            newlyBoughtCardIds = Set.copyOf(Objects.requireNonNull(newlyBoughtCardIds, "Newly bought card ids cannot be null"));
            pendingDiscardPlayerIds = Set.copyOf(Objects.requireNonNull(
                    pendingDiscardPlayerIds,
                    "Pending discard player ids cannot be null"
            ));
        }

        static TurnSaveData fromTurnState(TurnState turnState) {
            return new TurnSaveData(
                    turnState.getPhase(),
                    turnState.getSetupRound(),
                    turnState.hasRolledDice(),
                    turnState.hasPlayedDevelopmentCard(),
                    turnState.getRemainingSeconds(),
                    turnState.isWaitingForSetupPipe(),
                    turnState.getSetupPostIntersectionId(),
                    turnState.getNewlyBoughtCardIds(),
                    turnState.getPendingDiscardPlayerIds(),
                    turnState.isNimonMovedThisSeven()
            );
        }

        TurnState toTurnState(int currentPlayerIndex) {
            return TurnState.restore(
                    currentPlayerIndex,
                    phase,
                    setupRound,
                    hasRolledDice,
                    hasPlayedDevelopmentCard,
                    remainingSeconds,
                    waitingForSetupPipe,
                    setupPostIntersectionId,
                    newlyBoughtCardIds,
                    pendingDiscardPlayerIds,
                    nimonMovedThisSeven
            );
        }

        Map<String, Object> toSummaryMap() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("phase", phase);
            summary.put("setupRound", setupRound);
            summary.put("hasRolledDice", hasRolledDice);
            summary.put("hasPlayedDevelopmentCard", hasPlayedDevelopmentCard);
            summary.put("remainingSeconds", remainingSeconds);
            summary.put("waitingForSetupPipe", waitingForSetupPipe);
            summary.put("setupPostIntersectionId", setupPostIntersectionId);
            summary.put("newlyBoughtCardIds", newlyBoughtCardIds.stream().sorted().toList());
            summary.put("pendingDiscardPlayerIds", pendingDiscardPlayerIds.stream().sorted().toList());
            summary.put("nimonMovedThisSeven", nimonMovedThisSeven);
            return summary;
        }
    }
}
