package com.bananarepublic.model.harbor;

import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.resource.ResourceType;

public class WheatHarbor extends SpecificHarbor {

    public WheatHarbor(String id, Path attachedPath) {
        super(id, attachedPath, ResourceType.WHEAT);
    }

    @Override
    public String getDisplayName() {
        return "Pelabuhan Gandum 2:1";
    }
}