package com.bananarepublic.plugin;

import com.bananarepublic.model.board.Board;
import com.bananarepublic.service.board.StandardBoardFactory;

public class StandardMapGenerator implements MapGeneratorPlugin {
    @Override
    public Board generateBoard() {
        return new StandardBoardFactory().createBoard();
    }
}
