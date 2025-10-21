package org.shotrush.atom.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public final class AtomConfig {
    
    private final Map<String, Integer> xpRates;
    private final Map<String, Double> penaltyThresholds;
    private final Map<String, Double> bonusThresholds;
    private final int autoSaveInterval;
    private final boolean enablePenalties;
    private final boolean enableBonuses;
    private final boolean enableFeedback;
    private final boolean enableToolReinforcement;
    private final boolean enableXpTransfer;
    private final double parentXpMultiplier;
    private final double parentXpDecay;
    private final Map<Integer, Integer> depthXpRequirements;
    private final double advancementGrantThreshold;
    private final boolean enableDynamicTreeGeneration;
    
    private AtomConfig(Builder builder) {
        this.xpRates = Map.copyOf(builder.xpRates);
        this.penaltyThresholds = Map.copyOf(builder.penaltyThresholds);
        this.bonusThresholds = Map.copyOf(builder.bonusThresholds);
        this.autoSaveInterval = builder.autoSaveInterval;
        this.enablePenalties = builder.enablePenalties;
        this.enableBonuses = builder.enableBonuses;
        this.enableFeedback = builder.enableFeedback;
        this.enableToolReinforcement = builder.enableToolReinforcement;
        this.enableXpTransfer = builder.enableXpTransfer;
        this.parentXpMultiplier = builder.parentXpMultiplier;
        this.parentXpDecay = builder.parentXpDecay;
        this.depthXpRequirements = Map.copyOf(builder.depthXpRequirements);
        this.advancementGrantThreshold = builder.advancementGrantThreshold;
        this.enableDynamicTreeGeneration = builder.enableDynamicTreeGeneration;
    }
    
    public int getXpRate(String actionId) {
        return xpRates.getOrDefault(actionId, 10);
    }
    
    public double getPenaltyThreshold(String category) {
        return penaltyThresholds.getOrDefault(category, 10.0);
    }
    
    public double getBonusThreshold(String category) {
        return bonusThresholds.getOrDefault(category, 50.0);
    }
    
    public int autoSaveInterval() {
        return autoSaveInterval;
    }
    
    public boolean enablePenalties() {
        return enablePenalties;
    }
    
    public boolean enableBonuses() {
        return enableBonuses;
    }
    
    public boolean enableFeedback() {
        return enableFeedback;
    }
    
    public boolean enableToolReinforcement() {
        return enableToolReinforcement;
    }
    
    public boolean enableXpTransfer() {
        return enableXpTransfer;
    }
    
    public double parentXpMultiplier() {
        return parentXpMultiplier;
    }
    
    public double parentXpDecay() {
        return parentXpDecay;
    }
    
    public int getDepthXpRequirement(int depth) {
        return depthXpRequirements.getOrDefault(depth, 10000);
    }
    
    public double advancementGrantThreshold() {
        return advancementGrantThreshold;
    }
    
    public boolean enableDynamicTreeGeneration() {
        return enableDynamicTreeGeneration;
    }
    
    public static AtomConfig loadFrom(FileConfiguration config) {
        Builder builder = new Builder();
        
        ConfigurationSection xpSection = config.getConfigurationSection("xp-rates");
        if (xpSection != null) {
            for (String key : xpSection.getKeys(false)) {
                builder.xpRate(key, xpSection.getInt(key));
            }
        }
        
        ConfigurationSection penaltySection = config.getConfigurationSection("penalty-thresholds");
        if (penaltySection != null) {
            for (String key : penaltySection.getKeys(false)) {
                builder.penaltyThreshold(key, penaltySection.getDouble(key));
            }
        }
        
        ConfigurationSection bonusSection = config.getConfigurationSection("bonus-thresholds");
        if (bonusSection != null) {
            for (String key : bonusSection.getKeys(false)) {
                builder.bonusThreshold(key, bonusSection.getDouble(key));
            }
        }
        
        builder.autoSaveInterval(config.getInt("auto-save-interval", 300));
        builder.enablePenalties(config.getBoolean("features.penalties", true));
        builder.enableBonuses(config.getBoolean("features.bonuses", true));
        builder.enableFeedback(config.getBoolean("features.feedback", true));
        builder.enableToolReinforcement(config.getBoolean("features.tool-reinforcement", true));
        builder.enableXpTransfer(config.getBoolean("features.xp-transfer", true));
        builder.parentXpMultiplier(config.getDouble("parent-xp.multiplier", 0.1));
        builder.parentXpDecay(config.getDouble("parent-xp.decay", 0.5));
        
        builder.depthXpRequirement(1, config.getInt("depth-xp-requirements.depth-1", 1000));
        builder.depthXpRequirement(2, config.getInt("depth-xp-requirements.depth-2", 5000));
        builder.depthXpRequirement(3, config.getInt("depth-xp-requirements.depth-3", 10000));
        builder.depthXpRequirement(4, config.getInt("depth-xp-requirements.depth-4", 15000));
        
        builder.advancementGrantThreshold(config.getDouble("advancement-grant-threshold", 0.01));
        builder.enableDynamicTreeGeneration(config.getBoolean("features.dynamic-tree-generation", false));
        
        return builder.build();
    }
    
    public static final class Builder {
        private final Map<String, Integer> xpRates = new HashMap<>();
        private final Map<String, Double> penaltyThresholds = new HashMap<>();
        private final Map<String, Double> bonusThresholds = new HashMap<>();
        private int autoSaveInterval = 300;
        private boolean enablePenalties = true;
        private boolean enableBonuses = true;
        private boolean enableFeedback = true;
        private boolean enableToolReinforcement = true;
        private boolean enableXpTransfer = true;
        private double parentXpMultiplier = 0.1;
        private double parentXpDecay = 0.5;
        private final Map<Integer, Integer> depthXpRequirements = new HashMap<>();
        private double advancementGrantThreshold = 0.01;
        private boolean enableDynamicTreeGeneration = false;
        
        public Builder xpRate(String actionId, int rate) {
            this.xpRates.put(actionId, rate);
            return this;
        }
        
        public Builder penaltyThreshold(String category, double threshold) {
            this.penaltyThresholds.put(category, threshold);
            return this;
        }
        
        public Builder bonusThreshold(String category, double threshold) {
            this.bonusThresholds.put(category, threshold);
            return this;
        }
        
        public Builder autoSaveInterval(int seconds) {
            this.autoSaveInterval = seconds;
            return this;
        }
        
        public Builder enablePenalties(boolean enable) {
            this.enablePenalties = enable;
            return this;
        }
        
        public Builder enableBonuses(boolean enable) {
            this.enableBonuses = enable;
            return this;
        }
        
        public Builder enableFeedback(boolean enable) {
            this.enableFeedback = enable;
            return this;
        }
        
        public Builder enableToolReinforcement(boolean enable) {
            this.enableToolReinforcement = enable;
            return this;
        }
        
        public Builder enableXpTransfer(boolean enable) {
            this.enableXpTransfer = enable;
            return this;
        }
        
        public Builder parentXpMultiplier(double multiplier) {
            this.parentXpMultiplier = multiplier;
            return this;
        }
        
        public Builder parentXpDecay(double decay) {
            this.parentXpDecay = decay;
            return this;
        }
        
        public Builder depthXpRequirement(int depth, int xp) {
            this.depthXpRequirements.put(depth, xp);
            return this;
        }
        
        public Builder advancementGrantThreshold(double threshold) {
            this.advancementGrantThreshold = threshold;
            return this;
        }
        
        public Builder enableDynamicTreeGeneration(boolean enable) {
            this.enableDynamicTreeGeneration = enable;
            return this;
        }
        
        public AtomConfig build() {
            return new AtomConfig(this);
        }
    }
}
