package com.bananarepublic.plugin;

import com.bananarepublic.engine.GameEngine;

public interface Action {
    void execute(GameEngine engine, String playerId);

    default String describe() {
        return getClass().getSimpleName();
    }
}
