package com.bananarepublic.model.harbor;

import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.resource.ResourceType;

public class BrickHarbor extends SpecificHarbor {

    public BrickHarbor(String id, Path attachedPath) {
        super(id, attachedPath, ResourceType.BRICK);
    }

    @Override
    public String getDisplayName() {
        return "Pelabuhan Batu Bata 2:1";
    }
}