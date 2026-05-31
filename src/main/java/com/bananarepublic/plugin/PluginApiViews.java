package com.bananarepublic.plugin;

import com.bananarepublic.model.resource.ResourceType;

import java.util.List;
import java.util.Objects;

final class PluginApiViews {
    private PluginApiViews() {
    }

    static GameState stateView(com.bananarepublic.engine.GameState delegate) {
        return new GameStateView(delegate);
    }

    static Player playerView(com.bananarepublic.model.player.Player delegate) {
        return new PlayerView(delegate);
    }

    private record GameStateView(com.bananarepublic.engine.GameState delegate) implements GameState {
        private GameStateView {
            Objects.requireNonNull(delegate, "Game state delegate cannot be null");
        }

        @Override
        public List<Player> getAllPlayers() {
            return delegate.getPlayers().stream()
                    .map(PlayerView::new)
                    .map(player -> (Player) player)
                    .toList();
        }

        @Override
        public Bank getBank() {
            return new BankView(delegate.getBank());
        }
    }

    private record PlayerView(com.bananarepublic.model.player.Player delegate) implements Player {
        private PlayerView {
            Objects.requireNonNull(delegate, "Player delegate cannot be null");
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public int getResourceCount(ResourceType type) {
            return delegate.getResourceAmount(type);
        }

        @Override
        public void addResource(ResourceType type, int amount) {
            delegate.addResource(type, amount);
        }

        @Override
        public void removeResource(ResourceType type, int amount) {
            delegate.removeResource(type, amount);
        }
    }

    private record BankView(com.bananarepublic.model.resource.Bank delegate) implements Bank {
        private BankView {
            Objects.requireNonNull(delegate, "Bank delegate cannot be null");
        }

        @Override
        public boolean hasResource(ResourceType type, int amount) {
            return delegate.hasResource(type, amount);
        }

        @Override
        public void takeResource(ResourceType type, int amount) {
            delegate.take(type, amount);
        }
    }
}
