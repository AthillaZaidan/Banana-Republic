package com.bananarepublic.model.resource;

import java.util.Objects;

public class Bank {
    public static final int INITIAL_RESOURCE_AMOUNT = 19;

    private final ResourceInventory inventory;

    public Bank() {
        this.inventory = new ResourceInventory();

        for (ResourceType type : ResourceType.values()) {
            inventory.add(type, INITIAL_RESOURCE_AMOUNT);
        }
    }

    public int getAmount(ResourceType type) {
        return inventory.getAmount(type);
    }

    public boolean hasResource(ResourceType type, int amount) {
        return inventory.hasEnough(type, amount);
    }

    public boolean hasEnough(ResourceInventory requiredResources) {
        return inventory.hasEnough(requiredResources);
    }

    public void take(ResourceType type, int amount) {
        inventory.remove(type, amount);
    }

    public void take(ResourceInventory resourcesToTake) {
        Objects.requireNonNull(resourcesToTake, "Resources to take cannot be null");
        inventory.removeAll(resourcesToTake);
    }

    public void returnResource(ResourceType type, int amount) {
        inventory.add(type, amount);
    }

    public void returnResources(ResourceInventory returnedResources) {
        Objects.requireNonNull(returnedResources, "Returned resources cannot be null");
        inventory.addAll(returnedResources);
    }

    public ResourceInventory getInventoryCopy() {
        return inventory.copy();
    }
}