package org.shotrush.atom.manager;

import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.PlayerData;

public class EfficiencyManager {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;

    public EfficiencyManager(Atom plugin, ConfigManager configManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
    }

    public double getEfficiency(Player player, String actionId) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        double experience = data.getExperience(actionId);
        
        double baseEfficiency = configManager.getBaseEfficiency();
        double maxEfficiency = configManager.getMaxEfficiency();
        double expPerPoint = configManager.getExperiencePerEfficiencyPoint();
        
        double efficiency = baseEfficiency + (experience / expPerPoint);
        return Math.min(efficiency, maxEfficiency);
    }

    public double getBreakSpeedMultiplier(Player player, String actionId) {
        return getEfficiency(player, actionId);
    }

    public double getDropMultiplier(Player player, String actionId) {
        double efficiency = getEfficiency(player, actionId);
        return 1.0 + ((efficiency - 1.0) * 0.5);
    }

    public double getDamageMultiplier(Player player, String actionId) {
        return getEfficiency(player, actionId);
    }

    public int getFortuneBonus(Player player, String actionId) {
        double efficiency = getEfficiency(player, actionId);
        if (efficiency >= 1.5) return 2;
        if (efficiency >= 1.2) return 1;
        return 0;
    }

    public double getReplantChance(Player player, String actionId) {
        double efficiency = getEfficiency(player, actionId);
        return Math.min((efficiency - 1.0) * 0.5, 0.5);
    }

    public double getBreedingCooldownReduction(Player player, String actionId) {
        double efficiency = getEfficiency(player, actionId);
        return Math.min((efficiency - 1.0) * 0.3, 0.6);
    }

    public double getOffspringBonus(Player player, String actionId) {
        double efficiency = getEfficiency(player, actionId);
        return Math.min((efficiency - 1.0) * 0.2, 0.3);
    }

    public double getDurabilityCostReduction(Player player, String actionId) {
        double efficiency = getEfficiency(player, actionId);
        return Math.min((efficiency - 1.0) * 0.5, 0.5);
    }

    public double getCraftSpeedMultiplier(Player player, String actionId) {
        return getEfficiency(player, actionId);
    }

    public double getDurabilityBonus(Player player, String actionId) {
        double efficiency = getEfficiency(player, actionId);
        return 1.0 + ((efficiency - 1.0) * 0.5);
    }

    public double getFuelEfficiency(Player player, String actionId) {
        double efficiency = getEfficiency(player, actionId);
        return 1.0 + ((efficiency - 1.0) * 0.3);
    }

    public double getSmeltSpeedMultiplier(Player player, String actionId) {
        return getEfficiency(player, actionId);
    }

    public double getDiscount(Player player, String actionId) {
        double efficiency = getEfficiency(player, actionId);
        return Math.min((efficiency - 1.0) * 0.3, 0.5);
    }

    public double getReputationBonus(Player player, String actionId) {
        return getEfficiency(player, actionId);
    }

    public int getLevelCostReduction(Player player, String actionId) {
        double efficiency = getEfficiency(player, actionId);
        return (int) Math.min((efficiency - 1.0) * 10, 15);
    }

    public int getBetterEnchantsBonus(Player player, String actionId) {
        double efficiency = getEfficiency(player, actionId);
        if (efficiency >= 1.8) return 3;
        if (efficiency >= 1.5) return 2;
        if (efficiency >= 1.2) return 1;
        return 0;
    }
}
