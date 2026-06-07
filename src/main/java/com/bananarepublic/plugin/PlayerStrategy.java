package com.bananarepublic.plugin;

import com.bananarepublic.engine.GameState;

public interface PlayerStrategy {
    Action takeTurn(GameState state);
}
