package com.bananarepublic.model.card;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.player.Player;

public abstract class DevelopmentCard implements PlayableCard {
    private final String id;
    private final String name;
    private final String description;
    private final boolean hidden;

    protected DevelopmentCard(String id, String name, String description, boolean hidden) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Card id cannot be empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Card name cannot be empty");
        }
        this.id = id;
        this.name = name;
        this.description = description != null ? description : "";
        this.hidden = hidden;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Override
    public abstract boolean canPlay(GameState state, Player player);

    @Override
    public abstract void play(GameState state, Player player);
}
