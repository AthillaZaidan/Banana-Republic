package com.bananarepublic.model.building;

import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.player.Player;

import java.util.Objects;

public abstract class Building {
    private final Player owner;
    private final Intersection location;

    protected Building(Player owner, Intersection location) {
        this.owner = Objects.requireNonNull(owner, "Building owner cannot be null");
        this.location = Objects.requireNonNull(location, "Building location cannot be null");
    }

    public Player getOwner() {
        return owner;
    }

    public Intersection getLocation() {
        return location;
    }

    public abstract BuildingType getType();

    public int getVictoryPoint() {
        return getType().getVictoryPoint();
    }

    public int getProductionAmount() {
        return getType().getProductionAmount();
    }

    public String getDisplayName() {
        return getType().getDisplayName();
    }

    public abstract boolean canBeUpgraded();

    public boolean isOwnedBy(Player player) {
        return owner.equals(player);
    }
}