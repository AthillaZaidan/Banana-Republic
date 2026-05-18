package com.bananarepublic.model.building;

public enum BuildingType {
    MONITORING_POST("Pos Pantau", 1, 1),
    LABORATORY("Laboratorium", 2, 2);

    private final String displayName;
    private final int victoryPoint;
    private final int productionAmount;

    BuildingType(String displayName, int victoryPoint, int productionAmount) {
        this.displayName = displayName;
        this.victoryPoint = victoryPoint;
        this.productionAmount = productionAmount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getVictoryPoint() {
        return victoryPoint;
    }

    public int getProductionAmount() {
        return productionAmount;
    }
}