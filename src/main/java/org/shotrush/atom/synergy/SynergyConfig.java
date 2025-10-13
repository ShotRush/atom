package org.shotrush.atom.synergy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.shotrush.atom.skill.SkillType;

import java.util.*;
import java.util.regex.Pattern;

public class SynergyConfig {
    private boolean enabled;
    private double baseTransferRate;
    private Map<SkillType, List<SkillSynergy>> skillSynergies;
    private ItemSynergyConfig itemSynergies;

    public static class SkillSynergy {
        public SkillType targetSkill;
        public Pattern itemPattern;
        public double transferRate;

        public SkillSynergy(SkillType targetSkill, Pattern itemPattern, double transferRate) {
            this.targetSkill = targetSkill;
            this.itemPattern = itemPattern;
            this.transferRate = transferRate;
        }
    }

    public static class ItemSynergyPattern {
        public Pattern sourcePattern;
        public Pattern targetPattern;
        public double transferRate;

        public ItemSynergyPattern(Pattern sourcePattern, Pattern targetPattern, double transferRate) {
            this.sourcePattern = sourcePattern;
            this.targetPattern = targetPattern;
            this.transferRate = transferRate;
        }
    }

    public static class ItemSynergyConfig {
        public boolean enabled;
        public double sameSkillTransferRate;
        public List<ItemSynergyPattern> patterns;

        public ItemSynergyConfig(boolean enabled, double sameSkillTransferRate, List<ItemSynergyPattern> patterns) {
            this.enabled = enabled;
            this.sameSkillTransferRate = sameSkillTransferRate;
            this.patterns = patterns;
        }
    }

    public void loadFromConfig(FileConfiguration config) {
        ConfigurationSection synergySection = config.getConfigurationSection("synergy");
        if (synergySection == null) {
            enabled = false;
            baseTransferRate = 0.25;
            skillSynergies = new EnumMap<>(SkillType.class);
            itemSynergies = new ItemSynergyConfig(false, 0.15, new ArrayList<>());
            return;
        }

        enabled = synergySection.getBoolean("enabled", true);
        baseTransferRate = synergySection.getDouble("base-transfer-rate", 0.25);
        skillSynergies = new EnumMap<>(SkillType.class);

        ConfigurationSection skillSynSection = synergySection.getConfigurationSection("skill-synergies");
        if (skillSynSection != null) {
            for (String skillName : skillSynSection.getKeys(false)) {
                try {
                    SkillType sourceSkill = SkillType.valueOf(skillName.toUpperCase());
                    List<SkillSynergy> synergies = new ArrayList<>();

                    List<Map<?, ?>> synergyList = skillSynSection.getMapList(skillName);
                    for (Map<?, ?> synergyMap : synergyList) {
                        String targetSkillName = (String) synergyMap.get("target-skill");
                        String itemPatternStr = (String) synergyMap.get("item-pattern");
                        Object transferRateObj = synergyMap.get("transfer-rate");

                        if (targetSkillName != null && itemPatternStr != null && transferRateObj != null) {
                            try {
                                SkillType targetSkill = SkillType.valueOf(targetSkillName.toUpperCase());
                                Pattern itemPattern = Pattern.compile(itemPatternStr);
                                double transferRate = ((Number) transferRateObj).doubleValue();
                                synergies.add(new SkillSynergy(targetSkill, itemPattern, transferRate));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }

                    if (!synergies.isEmpty()) {
                        skillSynergies.put(sourceSkill, synergies);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        ConfigurationSection itemSynSection = synergySection.getConfigurationSection("item-synergies");
        if (itemSynSection != null) {
            boolean itemEnabled = itemSynSection.getBoolean("enabled", true);
            double sameSkillRate = itemSynSection.getDouble("same-skill-transfer-rate", 0.15);
            List<ItemSynergyPattern> patterns = new ArrayList<>();

            List<Map<?, ?>> patternList = itemSynSection.getMapList("patterns");
            for (Map<?, ?> patternMap : patternList) {
                String sourcePatternStr = (String) patternMap.get("source-pattern");
                String targetPatternStr = (String) patternMap.get("target-pattern");
                Object transferRateObj = patternMap.get("transfer-rate");

                if (sourcePatternStr != null && targetPatternStr != null && transferRateObj != null) {
                    try {
                        Pattern sourcePattern = Pattern.compile(sourcePatternStr);
                        Pattern targetPattern = Pattern.compile(targetPatternStr);
                        double transferRate = ((Number) transferRateObj).doubleValue();
                        patterns.add(new ItemSynergyPattern(sourcePattern, targetPattern, transferRate));
                    } catch (Exception ignored) {
                    }
                }
            }

            itemSynergies = new ItemSynergyConfig(itemEnabled, sameSkillRate, patterns);
        } else {
            itemSynergies = new ItemSynergyConfig(false, 0.15, new ArrayList<>());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getBaseTransferRate() {
        return baseTransferRate;
    }

    public Map<SkillType, List<SkillSynergy>> getSkillSynergies() {
        return skillSynergies;
    }

    public ItemSynergyConfig getItemSynergies() {
        return itemSynergies;
    }
}
