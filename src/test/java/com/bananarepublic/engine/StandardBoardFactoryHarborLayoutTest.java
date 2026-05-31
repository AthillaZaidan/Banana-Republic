package com.bananarepublic.engine;

import com.bananarepublic.model.board.Board;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.harbor.Harbor;
import com.bananarepublic.service.board.StandardBoardFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardBoardFactoryHarborLayoutTest {

    @Test
    void harborsUseDistinctCoastalPathsWithoutSharedIntersections() {
        Board board = new StandardBoardFactory().createBoard();
        List<Harbor> harbors = board.getHarbors().stream().toList();

        assertEquals(9, harbors.size());
        assertTrue(harbors.stream().allMatch(harbor -> harbor.getAttachedPath().isCoastalPath()));

        for (int i = 0; i < harbors.size(); i++) {
            Path left = harbors.get(i).getAttachedPath();
            for (int j = i + 1; j < harbors.size(); j++) {
                Path right = harbors.get(j).getAttachedPath();
                String message = harbors.get(i).getId() + " and " + harbors.get(j).getId() + " share a coastal intersection";
                assertFalse(sharesIntersection(left, right),
                        () -> message);
            }
        }
    }

    private boolean sharesIntersection(Path first, Path second) {
        return first.getEndpointA() == second.getEndpointA()
                || first.getEndpointA() == second.getEndpointB()
                || first.getEndpointB() == second.getEndpointA()
                || first.getEndpointB() == second.getEndpointB();
    }
}
