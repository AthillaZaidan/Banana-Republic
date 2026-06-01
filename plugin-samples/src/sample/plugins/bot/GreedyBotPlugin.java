package sample.plugins.bot;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.plugin.Action;
import com.bananarepublic.plugin.GreedyBotStrategy;
import com.bananarepublic.plugin.PlayerStrategy;

public final class GreedyBotPlugin implements PlayerStrategy {
    private final GreedyBotStrategy delegate = new GreedyBotStrategy();

    @Override
    public Action takeTurn(GameState state) {
        return delegate.takeTurn(state);
    }
}
