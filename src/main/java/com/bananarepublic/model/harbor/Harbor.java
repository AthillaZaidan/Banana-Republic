package com.bananarepublic.model.harbor;

import java.util.Objects;

import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.building.Building;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.ResourceType;

public abstract class Harbor {
    private final String id;
    private final Path attachedPath;

    protected Harbor(String id, Path attachedPath) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Harbor id cannot be empty");
        }

        this.id = id;
        this.attachedPath = Objects.requireNonNull(attachedPath, "Attached path cannot be null");
    }

    public String getId() {
        return id;
    }

    public Path getAttachedPath() {
        return attachedPath;
    }

    public abstract String getDisplayName();

    public abstract int getRatio();

    public abstract boolean canTradeWith(ResourceType offeredResource);

    public boolean isAccessibleBy(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        return hasBuildingOwnedBy(attachedPath.getEndpointA(), player)
                || hasBuildingOwnedBy(attachedPath.getEndpointB(), player);
    }

    private boolean hasBuildingOwnedBy(Intersection intersection, Player player) {
        return intersection.getBuilding()
                .map(Building::getOwner)
                .map(owner -> owner.equals(player))
                .orElse(false);
    }
}