package org.shotrush.atom.model;

public class EfficiencyBonus {
    private final String actionId;
    private final boolean breakSpeedMultiplier;
    private final boolean dropMultiplier;
    private final boolean durabilityCostReduction;
    private final boolean fortuneBonus;
    private final boolean replantChance;
    private final boolean breedingCooldownReduction;
    private final boolean offspringBonus;
    private final boolean damageMultiplier;
    private final boolean experienceMultiplier;
    private final boolean placementSpeed;
    private final boolean craftSpeed;
    private final boolean durabilityBonus;
    private final boolean fuelEfficiency;
    private final boolean smeltSpeed;
    private final boolean discount;
    private final boolean reputationBonus;
    private final boolean levelCostReduction;
    private final boolean betterEnchants;
    private final boolean treasureChance;

    public EfficiencyBonus(String actionId, boolean breakSpeedMultiplier, boolean dropMultiplier,
                          boolean durabilityCostReduction, boolean fortuneBonus, boolean replantChance,
                          boolean breedingCooldownReduction, boolean offspringBonus, boolean damageMultiplier,
                          boolean experienceMultiplier, boolean placementSpeed, boolean craftSpeed,
                          boolean durabilityBonus, boolean fuelEfficiency, boolean smeltSpeed,
                          boolean discount, boolean reputationBonus, boolean levelCostReduction,
                          boolean betterEnchants, boolean treasureChance) {
        this.actionId = actionId;
        this.breakSpeedMultiplier = breakSpeedMultiplier;
        this.dropMultiplier = dropMultiplier;
        this.durabilityCostReduction = durabilityCostReduction;
        this.fortuneBonus = fortuneBonus;
        this.replantChance = replantChance;
        this.breedingCooldownReduction = breedingCooldownReduction;
        this.offspringBonus = offspringBonus;
        this.damageMultiplier = damageMultiplier;
        this.experienceMultiplier = experienceMultiplier;
        this.placementSpeed = placementSpeed;
        this.craftSpeed = craftSpeed;
        this.durabilityBonus = durabilityBonus;
        this.fuelEfficiency = fuelEfficiency;
        this.smeltSpeed = smeltSpeed;
        this.discount = discount;
        this.reputationBonus = reputationBonus;
        this.levelCostReduction = levelCostReduction;
        this.betterEnchants = betterEnchants;
        this.treasureChance = treasureChance;
    }

    public String getActionId() {
        return actionId;
    }

    public boolean hasBreakSpeedMultiplier() {
        return breakSpeedMultiplier;
    }

    public boolean hasDropMultiplier() {
        return dropMultiplier;
    }

    public boolean hasDurabilityCostReduction() {
        return durabilityCostReduction;
    }

    public boolean hasFortuneBonus() {
        return fortuneBonus;
    }

    public boolean hasReplantChance() {
        return replantChance;
    }

    public boolean hasBreedingCooldownReduction() {
        return breedingCooldownReduction;
    }

    public boolean hasOffspringBonus() {
        return offspringBonus;
    }

    public boolean hasDamageMultiplier() {
        return damageMultiplier;
    }

    public boolean hasExperienceMultiplier() {
        return experienceMultiplier;
    }

    public boolean hasPlacementSpeed() {
        return placementSpeed;
    }

    public boolean hasCraftSpeed() {
        return craftSpeed;
    }

    public boolean hasDurabilityBonus() {
        return durabilityBonus;
    }

    public boolean hasFuelEfficiency() {
        return fuelEfficiency;
    }

    public boolean hasSmeltSpeed() {
        return smeltSpeed;
    }

    public boolean hasDiscount() {
        return discount;
    }

    public boolean hasReputationBonus() {
        return reputationBonus;
    }

    public boolean hasLevelCostReduction() {
        return levelCostReduction;
    }

    public boolean hasBetterEnchants() {
        return betterEnchants;
    }

    public boolean hasTreasureChance() {
        return treasureChance;
    }
}
