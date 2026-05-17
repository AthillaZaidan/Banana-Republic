package com.bananarepublic.model.harbor;

import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.resource.ResourceType;

public class GenericHarbor extends Harbor {

    public GenericHarbor(String id, Path attachedPath) {
        super(id, attachedPath);
    }

    @Override
    public String getDisplayName() {
        return "Pelabuhan Umum 3:1";
    }

    @Override
    public int getRatio() {
        return 3;
    }

    @Override
    public boolean canTradeWith(ResourceType offeredResource) {
        return offeredResource != null;
    }
}