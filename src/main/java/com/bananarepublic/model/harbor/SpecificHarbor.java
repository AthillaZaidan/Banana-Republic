package com.bananarepublic.model.harbor;

import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.resource.ResourceType;

import java.util.Objects;

public abstract class SpecificHarbor extends Harbor {
    private final ResourceType specificResource;

    protected SpecificHarbor(String id, Path attachedPath, ResourceType specificResource) {
        super(id, attachedPath);
        this.specificResource = Objects.requireNonNull(
                specificResource,
                "Specific resource cannot be null"
        );
    }

    public ResourceType getSpecificResource() {
        return specificResource;
    }

    @Override
    public int getRatio() {
        return 2;
    }

    @Override
    public boolean canTradeWith(ResourceType offeredResource) {
        return offeredResource == specificResource;
    }
}