package com.bananarepublic.model.transport;

import java.util.Objects;

import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.player.Player;

public class Pipe {
    private final String id;
    private final Player owner;
    private final Path location;

    public Pipe(String id, Player owner, Path location) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Pipe id cannot be empty");
        }

        this.id = id;
        this.owner = Objects.requireNonNull(owner, "Pipe owner cannot be null");
        this.location = Objects.requireNonNull(location, "Pipe location cannot be null");
    }

    public String getId() {
        return id;
    }

    public Player getOwner() {
        return owner;
    }

    public Path getLocation() {
        return location;
    }

    public boolean isOwnedBy(Player player) {
        return owner.equals(player);
    }
}