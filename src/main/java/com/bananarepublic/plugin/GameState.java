package com.bananarepublic.plugin;

import java.util.List;

public interface GameState {
    List<Player> getAllPlayers();
    Bank getBank();
}
