package org.shotrush.atom.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.shotrush.atom.skill.SkillType;

import java.util.HashMap;
import java.util.Map;

public class SkillConfig {
    private final Map<SkillType, SkillTypeConfig> configs = new HashMap<>();
    private double hungerCost;
    private MiscConfig misc;

    public static class MiscConfig {
        public boolean enableMushroomStew;

        public MiscConfig(boolean enableMushroomStew) {
            this.enableMushroomStew = enableMushroomStew;
        }
    }
    
    public static class SkillTypeConfig {
        public double baseSuccessRate;
        public double maxSuccessRate;
        public int experiencePerAction;
        public int experiencePerLevel;
        public int maxExperience;
        public boolean enabled;
        
        public SkillTypeConfig(double baseSuccessRate, double maxSuccessRate, int experiencePerAction, int experiencePerLevel, int maxExperience, boolean enabled) {
            this.baseSuccessRate = baseSuccessRate;
            this.maxSuccessRate = maxSuccessRate;
            this.experiencePerAction = experiencePerAction;
            this.experiencePerLevel = experiencePerLevel;
            this.maxExperience = maxExperience;
            this.enabled = enabled;
        }
    }
    
    public void loadFromConfig(FileConfiguration config) {
        hungerCost = config.getDouble("general.hunger-cost", 0.5);
        var miscSection = config.getConfigurationSection("misc");
        if(miscSection != null) {
            misc = new MiscConfig(miscSection.getBoolean("enable-mushroom-stew", true));
        } else {
            misc = new MiscConfig(true);
        }
        
        for (SkillType type : SkillType.values()) {
            String path = "skills." + type.name().toLowerCase();
            ConfigurationSection section = config.getConfigurationSection(path);
            
            if (section != null) {
                configs.put(type, new SkillTypeConfig(
                    section.getDouble("base-success-rate", 0.05),
                    section.getDouble("max-success-rate", 1.0),
                    section.getInt("experience-per-action", 1),
                    section.getInt("experience-per-level", 100),
                    section.getInt("max-experience", 1000),
                    section.getBoolean("enabled", true)
                ));
            } else {
                configs.put(type, new SkillTypeConfig(0.05, 1.0, 1, 100, 1000, true));
            }
        }
    }
    
    public SkillTypeConfig getConfig(SkillType type) {
        return configs.getOrDefault(type, new SkillTypeConfig(0.05, 1.0, 1, 100, 1000, true));
    }
    
    public double getHungerCost() {
        return hungerCost;
    }

    public MiscConfig getMisc() {
        return misc;
    }
}
