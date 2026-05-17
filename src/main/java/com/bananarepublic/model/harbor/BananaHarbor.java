package com.bananarepublic.model.harbor;

import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.resource.ResourceType;

public class BananaHarbor extends SpecificHarbor {

    public BananaHarbor(String id, Path attachedPath) {
        super(id, attachedPath, ResourceType.BANANA);
    }

    @Override
    public String getDisplayName() {
        return "Pelabuhan Pisang 2:1";
    }
}