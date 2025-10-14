package org.shotrush.atom.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.PlayerData;

import java.util.*;

public class SkillTransferManager {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final Map<String, Set<String>> skillGroups;
    private double transferRate;
    private boolean enabled;

    public SkillTransferManager(Atom plugin, ConfigManager configManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.skillGroups = new HashMap<>();
        loadConfig();
    }

    public void loadConfig() {
        ConfigurationSection transferSection = plugin.getConfig().getConfigurationSection("specialization.skill-transfer");
        if (transferSection == null) {
            enabled = false;
            return;
        }

        enabled = transferSection.getBoolean("enabled", true);
        transferRate = transferSection.getDouble("transfer-rate", 0.3);

        skillGroups.clear();
        ConfigurationSection relatedSkills = transferSection.getConfigurationSection("related-skills");
        if (relatedSkills != null) {
            for (String groupName : relatedSkills.getKeys(false)) {
                Set<String> skills = new HashSet<>(relatedSkills.getStringList(groupName));
                skillGroups.put(groupName, skills);
            }
        }
    }

    public double getEffectiveExperience(Player player, String actionId) {
        if (!enabled) {
            PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
            return data.getExperience(actionId);
        }

        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        double baseXP = data.getExperience(actionId);
        
        String skillGroup = findSkillGroup(actionId);
        if (skillGroup == null) {
            return baseXP;
        }

        Set<String> relatedActions = skillGroups.get(skillGroup);
        double transferredXP = 0.0;

        for (String relatedAction : relatedActions) {
            if (!relatedAction.equals(actionId)) {
                double relatedXP = data.getExperience(relatedAction);
                transferredXP += relatedXP * transferRate;
            }
        }

        return baseXP + transferredXP;
    }

    public double getVarietyBonus(Player player) {
        ConfigurationSection varietySection = plugin.getConfig().getConfigurationSection("specialization.variety-bonus");
        if (varietySection == null || !varietySection.getBoolean("enabled", true)) {
            return 0.0;
        }

        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        Map<String, Double> experience = data.getActionExperience();

        int actionsRequired = varietySection.getInt("actions-required", 5);
        long differentActions = experience.values().stream()
            .filter(xp -> xp > 50)
            .count();

        if (differentActions < actionsRequired) {
            return 0.0;
        }

        double bonusPerAction = varietySection.getDouble("bonus-per-different-action", 0.05);
        double maxBonus = varietySection.getDouble("max-variety-bonus", 0.5);

        return Math.min(differentActions * bonusPerAction, maxBonus);
    }

    public double getMasteryBonus(Player player, String actionId) {
        ConfigurationSection masterySection = plugin.getConfig().getConfigurationSection("specialization.mastery-bonus");
        if (masterySection == null || !masterySection.getBoolean("enabled", true)) {
            return 0.0;
        }

        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        double actionXP = data.getExperience(actionId);
        
        double masteryThreshold = masterySection.getDouble("mastery-threshold", 2000);
        
        double bonusPer1000 = masterySection.getDouble("bonus-per-1000-xp", 0.1);
        double maxBonus = masterySection.getDouble("max-mastery-bonus", 1.0);
        double exclusiveBonus = masterySection.getDouble("exclusive-bonus", 0.2);

        double masteryBonus = (actionXP / 1000.0) * bonusPer1000;
        
        if (actionXP < masteryThreshold) {
            return 0.0;
        }
        masteryBonus = Math.min(masteryBonus, maxBonus);

        double totalXP = data.getActionExperience().values().stream().mapToDouble(Double::doubleValue).sum();
        double specializationRatio = actionXP / totalXP;
        
        if (specializationRatio > 0.7) {
            masteryBonus += exclusiveBonus;
        }

        return masteryBonus;
    }

    private String findSkillGroup(String actionId) {
        for (Map.Entry<String, Set<String>> entry : skillGroups.entrySet()) {
            if (entry.getValue().contains(actionId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Map<String, Set<String>> getSkillGroups() {
        return new HashMap<>(skillGroups);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getTransferRate() {
        return transferRate;
    }
}
