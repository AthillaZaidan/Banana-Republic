package com.bananarepublic.model.player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.bananarepublic.model.building.Building;
import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.model.transport.Pipe;

public class Player {
    private final String id;
    private final String name;
    private final PlayerColor color;

    private final ResourceInventory resources;
    private final PlayerSupply supply;

    private final List<Building> ownedBuildings;
    private final List<Pipe> ownedPipes;
    private final Set<SpecialCardType> specialCards;

    private int playedKnightCount;
    private int secretVictoryPoints;

    public Player(String id, String name, PlayerColor color) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Player id cannot be empty");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name cannot be empty");
        }

        this.id = id;
        this.name = name;
        this.color = Objects.requireNonNull(color, "Player color cannot be null");

        this.resources = new ResourceInventory();
        this.supply = new PlayerSupply();

        this.ownedBuildings = new ArrayList<>();
        this.ownedPipes = new ArrayList<>();
        this.specialCards = EnumSet.noneOf(SpecialCardType.class);

        this.playedKnightCount = 0;
        this.secretVictoryPoints = 0;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PlayerColor getColor() {
        return color;
    }

    public ResourceInventory getResourceInventoryCopy() {
        return resources.copy();
    }

    public int getResourceAmount(ResourceType type) {
        return resources.getAmount(type);
    }

    public int getTotalResourceCards() {
        return resources.getTotalAmount();
    }

    public boolean hasResource(ResourceType type, int amount) {
        return resources.hasEnough(type, amount);
    }

    public boolean hasResources(ResourceInventory cost) {
        return resources.hasEnough(cost);
    }

    public void addResource(ResourceType type, int amount) {
        resources.add(type, amount);
    }

    public void addResources(ResourceInventory inventory) {
        resources.addAll(inventory);
    }

    public void removeResource(ResourceType type, int amount) {
        resources.remove(type, amount);
    }

    public void removeResources(ResourceInventory cost) {
        resources.removeAll(cost);
    }

    public PlayerSupply getSupply() {
        return supply;
    }

    public void registerBuilding(Building building) {
        Objects.requireNonNull(building, "Building cannot be null");

        if (!building.isOwnedBy(this)) {
            throw new IllegalArgumentException("Cannot register building owned by another player");
        }

        if (!ownedBuildings.contains(building)) {
            ownedBuildings.add(building);
        }
    }

    public void unregisterBuilding(Building building) {
        Objects.requireNonNull(building, "Building cannot be null");
        ownedBuildings.remove(building);
    }

    public List<Building> getOwnedBuildings() {
        return List.copyOf(ownedBuildings);
    }

    public void registerPipe(Pipe pipe) {
        Objects.requireNonNull(pipe, "Pipe cannot be null");

        if (!pipe.isOwnedBy(this)) {
            throw new IllegalArgumentException("Cannot register pipe owned by another player");
        }

        if (!ownedPipes.contains(pipe)) {
            ownedPipes.add(pipe);
        }
    }

    public void unregisterPipe(Pipe pipe) {
        Objects.requireNonNull(pipe, "Pipe cannot be null");
        ownedPipes.remove(pipe);
    }

    public List<Pipe> getOwnedPipes() {
        return List.copyOf(ownedPipes);
    }

    public int getPlayedKnightCount() {
        return playedKnightCount;
    }

    public void incrementPlayedKnightCount() {
        playedKnightCount++;
    }

    public int getSecretVictoryPoints() {
        return secretVictoryPoints;
    }

    public void addSecretVictoryPoints(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Secret victory points cannot be negative");
        }

        secretVictoryPoints += amount;
    }

    public void addSpecialCard(SpecialCardType specialCardType) {
        specialCards.add(Objects.requireNonNull(specialCardType, "Special card type cannot be null"));
    }

    public void removeSpecialCard(SpecialCardType specialCardType) {
        specialCards.remove(Objects.requireNonNull(specialCardType, "Special card type cannot be null"));
    }

    public boolean hasSpecialCard(SpecialCardType specialCardType) {
        return specialCards.contains(Objects.requireNonNull(specialCardType, "Special card type cannot be null"));
    }

    public Set<SpecialCardType> getSpecialCards() {
        return Set.copyOf(specialCards);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Player player)) {
            return false;
        }

        return id.equals(player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
