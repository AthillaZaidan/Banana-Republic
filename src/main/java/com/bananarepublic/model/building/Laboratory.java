package com.bananarepublic.model.building;

import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.player.Player;

public class Laboratory extends Building{
    public Laboratory(Player owner, Intersection location) {
        super(owner, location);
    }

    @Override
    public BuildingType getType() {
        return BuildingType.LABORATORY;
    }

    @Override
    public boolean canBeUpgraded() {
        return false;
    }
}