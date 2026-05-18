package com.bananarepublic.model.harbor;

import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.resource.ResourceType;

public class WoodHarbor extends SpecificHarbor {

    public WoodHarbor(String id, Path attachedPath) {
        super(id, attachedPath, ResourceType.WOOD);
    }

    @Override
    public String getDisplayName() {
        return "Pelabuhan Kayu 2:1";
    }
}