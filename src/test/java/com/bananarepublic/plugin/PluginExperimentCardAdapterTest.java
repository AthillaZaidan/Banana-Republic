package com.bananarepublic.plugin;

import com.bananarepublic.engine.BoardMode;
import com.bananarepublic.engine.GameConfig;
import com.bananarepublic.engine.GameEngine;
import com.bananarepublic.engine.PlayerConfig;
import com.bananarepublic.model.player.PlayerColor;
import com.bananarepublic.model.resource.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginExperimentCardAdapterTest {
    @Test
    void adapterExposesPluginApiContractViews() {
        GameEngine engine = new GameEngine();
        engine.startNewGame(new GameConfig(List.of(
                new PlayerConfig("Nimo", PlayerColor.RED),
                new PlayerConfig("Nero", PlayerColor.BLUE)
        ), BoardMode.FIXED, true));
        com.bananarepublic.model.player.Player active = engine.getState().getCurrentPlayer();
        active.addResource(ResourceType.WOOD, 1);
        int brickInBankBefore = engine.getState().getBank().getAmount(ResourceType.BRICK);

        ExperimentCard experimentCard = new ExperimentCard() {
            @Override
            public String getCardName() {
                return "Sample Plugin";
            }

            @Override
            public String getDescription() {
                return "Uses the plugin-facing API";
            }

            @Override
            public void applyEffect(GameState state, Player player) {
                assertEquals(active.getName(), player.getName());
                assertEquals(2, state.getAllPlayers().size());
                assertTrue(state.getBank().hasResource(ResourceType.BRICK, 1));
                state.getBank().takeResource(ResourceType.BRICK, 1);
                player.removeResource(ResourceType.WOOD, 1);
                player.addResource(ResourceType.BRICK, 1);
            }
        };

        PluginExperimentCardAdapter adapter = new PluginExperimentCardAdapter(experimentCard);
        adapter.play(engine.getState(), active);

        assertEquals(0, active.getResourceAmount(ResourceType.WOOD));
        assertEquals(1, active.getResourceAmount(ResourceType.BRICK));
        assertEquals(brickInBankBefore - 1, engine.getState().getBank().getAmount(ResourceType.BRICK));
    }
}
