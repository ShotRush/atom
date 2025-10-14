package org.shotrush.atom.manager;

import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.ActionContext;
import org.shotrush.atom.model.EmergentBonus;
import org.shotrush.atom.model.PlayerData;

import java.util.ArrayList;
import java.util.List;

public class EmergentBonusManager {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;

    public EmergentBonusManager(Atom plugin, ConfigManager configManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
    }

    public List<EmergentBonus> calculateBonuses(Player player, ActionContext context) {
        List<EmergentBonus> bonuses = new ArrayList<>();
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        double experience = data.getExperience(context.getActionId());
        
        double efficiency = calculateEfficiency(experience);
        
        if (context.hasBreakingComponent()) {
            bonuses.add(new EmergentBonus(
                "speed_" + context.getActionId(),
                EmergentBonus.BonusCategory.SPEED,
                efficiency,
                "Break/gather faster"
            ));
            
            if (experience > 500) {
                bonuses.add(new EmergentBonus(
                    "yield_" + context.getActionId(),
                    EmergentBonus.BonusCategory.YIELD,
                    1.0 + ((efficiency - 1.0) * 0.5),
                    "Get more drops"
                ));
            }
        }
        
        if (context.usesTool() && experience > 200) {
            bonuses.add(new EmergentBonus(
                "durability_" + context.getActionId(),
                EmergentBonus.BonusCategory.DURABILITY,
                Math.min((efficiency - 1.0) * 0.5, 0.5),
                "Tools last longer"
            ));
        }
        
        if (context.hasCombatComponent()) {
            bonuses.add(new EmergentBonus(
                "damage_" + context.getActionId(),
                EmergentBonus.BonusCategory.SPEED,
                efficiency,
                "Deal more damage"
            ));
        }
        
        if (context.hasCraftingComponent() && experience > 300) {
            bonuses.add(new EmergentBonus(
                "quality_" + context.getActionId(),
                EmergentBonus.BonusCategory.QUALITY,
                1.0 + ((efficiency - 1.0) * 0.3),
                "Create higher quality items"
            ));
        }
        
        if (context.hasProcessingComponent()) {
            bonuses.add(new EmergentBonus(
                "efficiency_" + context.getActionId(),
                EmergentBonus.BonusCategory.EFFICIENCY,
                efficiency,
                "Process faster with less fuel"
            ));
        }
        
        if (context.hasInteractionComponent() && experience > 400) {
            bonuses.add(new EmergentBonus(
                "discovery_" + context.getActionId(),
                EmergentBonus.BonusCategory.DISCOVERY,
                Math.min(experience / 1000.0, 0.5),
                "Unlock better outcomes"
            ));
        }
        
        return bonuses;
    }

    public double getSpeedMultiplier(Player player, String actionId) {
        double baseEfficiency = calculateBaseEfficiency(player, actionId);
        double varietyBonus = plugin.getSkillTransferManager().getVarietyBonus(player);
        double masteryBonus = plugin.getSkillTransferManager().getMasteryBonus(player, actionId);
        
        double totalEfficiency = Math.min(baseEfficiency + varietyBonus + masteryBonus, configManager.getMaxEfficiency());
        
        return plugin.getCognitiveCapacityManager().applyCognitiveLoad(player, actionId, totalEfficiency);
    }

    public double getYieldMultiplier(Player player, String actionId) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        double experience = data.getExperience(actionId);
        if (experience < 500) return 1.0;
        
        double efficiency = calculateEfficiency(experience);
        return 1.0 + ((efficiency - 1.0) * 0.5);
    }

    public double getDurabilityReduction(Player player, String actionId) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        double experience = data.getExperience(actionId);
        if (experience < 200) return 0.0;
        
        double efficiency = calculateEfficiency(experience);
        return Math.min((efficiency - 1.0) * 0.5, 0.5);
    }

    public double getQualityMultiplier(Player player, String actionId) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        double experience = data.getExperience(actionId);
        if (experience < 300) return 1.0;
        
        double efficiency = calculateEfficiency(experience);
        return 1.0 + ((efficiency - 1.0) * 0.3);
    }

    public double getDiscoveryChance(Player player, String actionId) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        double experience = data.getExperience(actionId);
        if (experience < 400) return 0.00001;
        
        double baseChance = 0.00001;
        double experienceAboveThreshold = experience - 400;
        double scalingFactor = Math.pow(1.0015, experienceAboveThreshold / 100.0);
        
        return Math.min(baseChance * scalingFactor, 0.05);
    }

    private double calculateEfficiency(double experience) {
        double baseEfficiency = configManager.getBaseEfficiency();
        double maxEfficiency = configManager.getMaxEfficiency();
        double expPerPoint = configManager.getExperiencePerEfficiencyPoint();
        
        double efficiency = baseEfficiency + (Math.sqrt(experience) / Math.sqrt(expPerPoint));
        return Math.min(efficiency, maxEfficiency);
    }

    private double calculateBaseEfficiency(Player player, String actionId) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        double experience = data.getExperience(actionId);
        return calculateEfficiency(experience);
    }
}
