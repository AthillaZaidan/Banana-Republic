package com.bananarepublic.model.harbor;

import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.resource.ResourceType;

public class OreHarbor extends SpecificHarbor {

    public OreHarbor(String id, Path attachedPath) {
        super(id, attachedPath, ResourceType.ORE);
    }

    @Override
    public String getDisplayName() {
        return "Pelabuhan Bijih 2:1";
    }
}