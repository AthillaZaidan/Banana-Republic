package com.bananarepublic.model.resource;

public enum TerrainType {
    FOREST(ResourceType.WOOD),
    HILL(ResourceType.BRICK),
    WHEAT_FIELD(ResourceType.WHEAT),
    MOUNTAIN(ResourceType.ORE),
    BANANA_PLANTATION(ResourceType.BANANA),
    DESERT(null);

    private final ResourceType producedResource;

    TerrainType(ResourceType producedResource) {
        this.producedResource = producedResource;
    }

    public ResourceType getProducedResource() {
        return producedResource;
    }

    public boolean producesResource() {
        return producedResource != null;
    }
}