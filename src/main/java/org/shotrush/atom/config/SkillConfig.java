package org.shotrush.atom.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.shotrush.atom.skill.SkillType;

import java.util.*;

public class SkillConfig {
    private final Map<SkillType, SkillTypeConfig> configs = new HashMap<>();
    private double hungerCost;
    private MiscConfig misc;
    private DecayConfig decay;
    private LearningBoostConfig learningBoost;

    public static class MiscConfig {
        public boolean enableMushroomStew;

        public MiscConfig(boolean enableMushroomStew) {
            this.enableMushroomStew = enableMushroomStew;
        }
    }

    public static class DecayConfig {
        public boolean enabled;
        public double initialHalfLifeHours;
        public double stabilityIncreasePerRepetition;
        public double maxStabilityMultiplier;
        public double minRetentionPercentage;

        public DecayConfig(boolean enabled, double initialHalfLifeHours, double stabilityIncreasePerRepetition, double maxStabilityMultiplier, double minRetentionPercentage) {
            this.enabled = enabled;
            this.initialHalfLifeHours = initialHalfLifeHours;
            this.stabilityIncreasePerRepetition = stabilityIncreasePerRepetition;
            this.maxStabilityMultiplier = maxStabilityMultiplier;
            this.minRetentionPercentage = minRetentionPercentage;
        }
    }

    public static class LearningBoostConfig {
        public MentorshipConfig mentorship;
        public BooksConfig books;
        public InfrastructureConfig infrastructure;

        public static class MentorshipConfig {
            public boolean enabled;
            public double radius;
            public double maxBoost;
            public int skillDifferenceThreshold;

            public MentorshipConfig(boolean enabled, double radius, double maxBoost, int skillDifferenceThreshold) {
                this.enabled = enabled;
                this.radius = radius;
                this.maxBoost = maxBoost;
                this.skillDifferenceThreshold = skillDifferenceThreshold;
            }
        }

        public static class BooksConfig {
            public boolean enabled;
            public double maxBoost;

            public BooksConfig(boolean enabled, double maxBoost) {
                this.enabled = enabled;
                this.maxBoost = maxBoost;
            }
        }

        public static class InfrastructureConfig {
            public boolean enabled;
            public double maxBoost;
            public int searchRadius;
            public Map<SkillType, List<Material>> blocks;

            public InfrastructureConfig(boolean enabled, double maxBoost, int searchRadius, Map<SkillType, List<Material>> blocks) {
                this.enabled = enabled;
                this.maxBoost = maxBoost;
                this.searchRadius = searchRadius;
                this.blocks = blocks;
            }
        }

        public LearningBoostConfig(MentorshipConfig mentorship, BooksConfig books, InfrastructureConfig infrastructure) {
            this.mentorship = mentorship;
            this.books = books;
            this.infrastructure = infrastructure;
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

        var decaySection = config.getConfigurationSection("decay");
        if(decaySection != null) {
            decay = new DecayConfig(
                decaySection.getBoolean("enabled", true),
                decaySection.getDouble("initial-half-life-hours", 24.0),
                decaySection.getDouble("stability-increase-per-repetition", 1.5),
                decaySection.getDouble("max-stability-multiplier", 10.0),
                decaySection.getDouble("min-retention-percentage", 0.1)
            );
        } else {
            decay = new DecayConfig(true, 24.0, 1.5, 10.0, 0.1);
        }

        var boostSection = config.getConfigurationSection("learning-boost");
        if(boostSection != null) {
            var mentorshipSection = boostSection.getConfigurationSection("mentorship");
            LearningBoostConfig.MentorshipConfig mentorship = mentorshipSection != null ?
                new LearningBoostConfig.MentorshipConfig(
                    mentorshipSection.getBoolean("enabled", true),
                    mentorshipSection.getDouble("radius", 10.0),
                    mentorshipSection.getDouble("max-boost", 2.0),
                    mentorshipSection.getInt("skill-difference-threshold", 100)
                ) : new LearningBoostConfig.MentorshipConfig(true, 10.0, 2.0, 100);

            var booksSection = boostSection.getConfigurationSection("books");
            LearningBoostConfig.BooksConfig books = booksSection != null ?
                new LearningBoostConfig.BooksConfig(
                    booksSection.getBoolean("enabled", true),
                    booksSection.getDouble("max-boost", 1.5)
                ) : new LearningBoostConfig.BooksConfig(true, 1.5);

            var infraSection = boostSection.getConfigurationSection("infrastructure");
            Map<SkillType, List<Material>> infraBlocks = new EnumMap<>(SkillType.class);
            if(infraSection != null) {
                var blocksSection = infraSection.getConfigurationSection("blocks");
                if(blocksSection != null) {
                    for(String key : blocksSection.getKeys(false)) {
                        try {
                            SkillType skillType = SkillType.valueOf(key.toUpperCase());
                            List<String> materialNames = blocksSection.getStringList(key);
                            List<Material> materials = new ArrayList<>();
                            for(String matName : materialNames) {
                                try {
                                    materials.add(Material.valueOf(matName));
                                } catch(IllegalArgumentException ignored) {
                                }
                            }
                            infraBlocks.put(skillType, materials);
                        } catch(IllegalArgumentException ignored) {
                        }
                    }
                }
            }
            LearningBoostConfig.InfrastructureConfig infrastructure = infraSection != null ?
                new LearningBoostConfig.InfrastructureConfig(
                    infraSection.getBoolean("enabled", true),
                    infraSection.getDouble("max-boost", 2.5),
                    infraSection.getInt("search-radius", 5),
                    infraBlocks
                ) : new LearningBoostConfig.InfrastructureConfig(true, 2.5, 5, infraBlocks);

            learningBoost = new LearningBoostConfig(mentorship, books, infrastructure);
        } else {
            learningBoost = new LearningBoostConfig(
                new LearningBoostConfig.MentorshipConfig(true, 10.0, 2.0, 100),
                new LearningBoostConfig.BooksConfig(true, 1.5),
                new LearningBoostConfig.InfrastructureConfig(true, 2.5, 5, new EnumMap<>(SkillType.class))
            );
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

    public DecayConfig getDecay() {
        return decay;
    }

    public LearningBoostConfig getLearningBoost() {
        return learningBoost;
    }
}
