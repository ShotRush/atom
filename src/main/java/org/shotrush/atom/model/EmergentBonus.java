package org.shotrush.atom.model;

public class EmergentBonus {
    private final String bonusId;
    private final BonusCategory category;
    private final double magnitude;
    private final String description;

    public EmergentBonus(String bonusId, BonusCategory category, double magnitude, String description) {
        this.bonusId = bonusId;
        this.category = category;
        this.magnitude = magnitude;
        this.description = description;
    }

    public String getBonusId() {
        return bonusId;
    }

    public BonusCategory getCategory() {
        return category;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public String getDescription() {
        return description;
    }

    public enum BonusCategory {
        SPEED,
        YIELD,
        EFFICIENCY,
        QUALITY,
        DURABILITY,
        DISCOVERY
    }
}
