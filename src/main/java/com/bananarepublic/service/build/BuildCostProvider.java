package com.bananarepublic.service.build;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;

public class BuildCostProvider {
    private final Map<BuildActionType, ResourceInventory> costs;

    public BuildCostProvider() {
        this.costs = new EnumMap<>(BuildActionType.class);

        costs.put(BuildActionType.PIPE, createPipeCost());
        costs.put(BuildActionType.MONITORING_POST, createMonitoringPostCost());
        costs.put(BuildActionType.LABORATORY, createLaboratoryCost());
        costs.put(BuildActionType.EXPERIMENT_CARD, createExperimentCardCost());
    }

    public ResourceInventory getCost(BuildActionType actionType) {
        Objects.requireNonNull(actionType, "Build action type cannot be null");

        ResourceInventory cost = costs.get(actionType);

        if (cost == null) {
            throw new IllegalArgumentException("Unknown build action type: " + actionType);
        }

        return cost.copy();
    }

    private ResourceInventory createPipeCost() {
        ResourceInventory cost = new ResourceInventory();
        cost.add(ResourceType.WOOD, 1);
        cost.add(ResourceType.BRICK, 1);
        return cost;
    }

    private ResourceInventory createMonitoringPostCost() {
        ResourceInventory cost = new ResourceInventory();
        cost.add(ResourceType.WOOD, 1);
        cost.add(ResourceType.BRICK, 1);
        cost.add(ResourceType.BANANA, 1);
        cost.add(ResourceType.WHEAT, 1);
        return cost;
    }

    private ResourceInventory createLaboratoryCost() {
        ResourceInventory cost = new ResourceInventory();
        cost.add(ResourceType.ORE, 3);
        cost.add(ResourceType.WHEAT, 2);
        return cost;
    }

    private ResourceInventory createExperimentCardCost() {
        ResourceInventory cost = new ResourceInventory();
        cost.add(ResourceType.ORE, 1);
        cost.add(ResourceType.BANANA, 1);
        cost.add(ResourceType.WHEAT, 1);
        return cost;
    }
}