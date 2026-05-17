package com.bananarepublic.model.building;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.player.Player;

public class MonitoringPost extends Building {
    public MonitoringPost(Player owner, Intersection location) {
        super(owner, location);
    }

    @Override
    public BuildingType getType() {
        return BuildingType.MONITORING_POST;
    }

    @Override
    public boolean canBeUpgraded() {
        return true;
    }
}