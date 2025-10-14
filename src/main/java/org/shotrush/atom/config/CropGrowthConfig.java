package org.shotrush.atom.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class CropGrowthConfig {
    private final Plugin plugin;
    
    private boolean enabled;
    private boolean applyToVanillaCrops;
    
    private final Map<Material, CropAffinityData> cropAffinities;
    private double optimalMultiplier;
    private double goodMultiplier;
    private double poorMultiplier;
    private double terribleMultiplier;
    
    private boolean foodSurplusEnabled;
    private double surplusThreshold;
    private double bonusPerSurplus;
    private double maxSurplusBonus;

    public CropGrowthConfig(Plugin plugin) {
        this.plugin = plugin;
        this.cropAffinities = new HashMap<>();
        loadConfig();
    }

    public void loadConfig() {
        ConfigurationSection cropSection = plugin.getConfig().getConfigurationSection("environmental-factors.crop-growth");
        if (cropSection == null) {
            setDefaults();
            return;
        }

        enabled = cropSection.getBoolean("enabled", true);
        applyToVanillaCrops = cropSection.getBoolean("apply-to-vanilla-crops", true);

        ConfigurationSection affinitySection = cropSection.getConfigurationSection("crop-biome-affinity");
        if (affinitySection != null) {
            for (String cropName : affinitySection.getKeys(false)) {
                Material cropMaterial = parseCropMaterial(cropName);
                if (cropMaterial == null) continue;

                ConfigurationSection cropData = affinitySection.getConfigurationSection(cropName);
                if (cropData == null) continue;

                CropAffinityData affinity = new CropAffinityData();
                affinity.optimal = new HashSet<>(cropData.getStringList("optimal"));
                affinity.good = new HashSet<>(cropData.getStringList("good"));
                affinity.poor = new HashSet<>(cropData.getStringList("poor"));
                affinity.terrible = new HashSet<>(cropData.getStringList("terrible"));

                cropAffinities.put(cropMaterial, affinity);
            }
        }

        ConfigurationSection multipliers = cropSection.getConfigurationSection("growth-multipliers");
        if (multipliers != null) {
            optimalMultiplier = multipliers.getDouble("optimal", 1.5);
            goodMultiplier = multipliers.getDouble("good", 1.2);
            poorMultiplier = multipliers.getDouble("poor", 0.7);
            terribleMultiplier = multipliers.getDouble("terrible", 0.4);
        }

        ConfigurationSection surplus = cropSection.getConfigurationSection("food-surplus-bonus");
        if (surplus != null) {
            foodSurplusEnabled = surplus.getBoolean("enabled", true);
            surplusThreshold = surplus.getDouble("surplus-threshold", 1.3);
            bonusPerSurplus = surplus.getDouble("bonus-per-surplus", 0.1);
            maxSurplusBonus = surplus.getDouble("max-bonus", 0.3);
        }
    }

    private void setDefaults() {
        enabled = true;
        applyToVanillaCrops = true;
        optimalMultiplier = 1.5;
        goodMultiplier = 1.2;
        poorMultiplier = 0.7;
        terribleMultiplier = 0.4;
        foodSurplusEnabled = true;
        surplusThreshold = 1.3;
        bonusPerSurplus = 0.1;
        maxSurplusBonus = 0.3;
    }

    private Material parseCropMaterial(String cropName) {
        switch (cropName.toLowerCase()) {
            case "wheat": return Material.WHEAT;
            case "carrots": return Material.CARROTS;
            case "potatoes": return Material.POTATOES;
            case "beetroots": return Material.BEETROOTS;
            case "melons": return Material.MELON_STEM;
            case "pumpkins": return Material.PUMPKIN_STEM;
            case "cocoa": return Material.COCOA;
            case "sugar_cane": return Material.SUGAR_CANE;
            default: return null;
        }
    }

    public double getGrowthMultiplier(Material crop, String biome) {
        if (!enabled) return 1.0;

        CropAffinityData affinity = cropAffinities.get(crop);
        if (affinity == null) return 1.0;

        if (affinity.optimal.contains(biome)) return optimalMultiplier;
        if (affinity.good.contains(biome)) return goodMultiplier;
        if (affinity.poor.contains(biome)) return poorMultiplier;
        if (affinity.terrible.contains(biome)) return terribleMultiplier;

        return 1.0;
    }

    public double getFoodSurplusBonus(double cropProductivity) {
        if (!foodSurplusEnabled) return 0.0;
        if (cropProductivity < surplusThreshold) return 0.0;

        double surplus = cropProductivity - surplusThreshold;
        return Math.min(surplus * bonusPerSurplus, maxSurplusBonus);
    }

    public boolean isEnabled() { return enabled; }
    public boolean shouldApplyToVanillaCrops() { return applyToVanillaCrops; }
    public double getOptimalMultiplier() { return optimalMultiplier; }
    public double getGoodMultiplier() { return goodMultiplier; }
    public double getPoorMultiplier() { return poorMultiplier; }
    public double getTerribleMultiplier() { return terribleMultiplier; }
    public boolean isFoodSurplusEnabled() { return foodSurplusEnabled; }
    public double getSurplusThreshold() { return surplusThreshold; }

    public static class CropAffinityData {
        public Set<String> optimal;
        public Set<String> good;
        public Set<String> poor;
        public Set<String> terrible;

        public CropAffinityData() {
            this.optimal = new HashSet<>();
            this.good = new HashSet<>();
            this.poor = new HashSet<>();
            this.terrible = new HashSet<>();
        }
    }
}
