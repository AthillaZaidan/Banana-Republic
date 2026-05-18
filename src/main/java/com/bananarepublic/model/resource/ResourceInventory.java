package com.bananarepublic.model.resource;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class ResourceInventory {
    private final Map<ResourceType, Integer> resources;

    public ResourceInventory() {
        this.resources = new EnumMap<>(ResourceType.class);

        for (ResourceType type : ResourceType.values()) {
            resources.put(type, 0);
        }
    }

    public ResourceInventory(ResourceInventory other) {
        this();

        Objects.requireNonNull(other, "Other inventory cannot be null");

        for (ResourceType type : ResourceType.values()) {
            resources.put(type, other.getAmount(type));
        }
    }

    public int getAmount(ResourceType type) {
        Objects.requireNonNull(type, "Resource type cannot be null");
        return resources.get(type);
    }

    public void add(ResourceType type, int amount) {
        Objects.requireNonNull(type, "Resource type cannot be null");
        validateNonNegative(amount);

        resources.put(type, getAmount(type) + amount);
    }

    public void addAll(ResourceInventory other) {
        Objects.requireNonNull(other, "Other inventory cannot be null");

        for (ResourceType type : ResourceType.values()) {
            add(type, other.getAmount(type));
        }
    }

    public void remove(ResourceType type, int amount) {
        Objects.requireNonNull(type, "Resource type cannot be null");
        validateNonNegative(amount);

        if (!hasEnough(type, amount)) {
            throw new IllegalArgumentException("Not enough resource: " + type);
        }

        resources.put(type, getAmount(type) - amount);
    }

    public void removeAll(ResourceInventory cost) {
        Objects.requireNonNull(cost, "Cost inventory cannot be null");

        if (!hasEnough(cost)) {
            throw new IllegalArgumentException("Not enough resources");
        }

        for (ResourceType type : ResourceType.values()) {
            remove(type, cost.getAmount(type));
        }
    }

    public boolean hasEnough(ResourceType type, int amount) {
        Objects.requireNonNull(type, "Resource type cannot be null");
        validateNonNegative(amount);

        return getAmount(type) >= amount;
    }

    public boolean hasEnough(ResourceInventory cost) {
        Objects.requireNonNull(cost, "Cost inventory cannot be null");

        for (ResourceType type : ResourceType.values()) {
            if (!hasEnough(type, cost.getAmount(type))) {
                return false;
            }
        }

        return true;
    }

    public boolean isEmpty() {
        return getTotalAmount() == 0;
    }

    public int getTotalAmount() {
        int total = 0;

        for (int amount : resources.values()) {
            total += amount;
        }

        return total;
    }

    public ResourceInventory copy() {
        return new ResourceInventory(this);
    }

    public Map<ResourceType, Integer> asMap() {
        return Map.copyOf(resources);
    }

    private void validateNonNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
}