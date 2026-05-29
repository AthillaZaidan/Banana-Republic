package com.bananarepublic.persistence;

import com.bananarepublic.engine.GameState;

import java.nio.file.Path;

public interface SaveLoadService {
    void save(GameState state, Path path);

    GameState load(Path path);
}
