package org.shotrush.atom.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.handler.HungerHandler;
import org.shotrush.atom.handler.SkillHandler;
import org.shotrush.atom.skill.SkillType;
import org.shotrush.atom.synergy.SynergyCalculator;

import java.util.List;
import java.util.UUID;

public class AtomAPI {
    private static Atom plugin;
    
    public static void initialize(Atom atomPlugin) {
        plugin = atomPlugin;
    }
    
    public static Atom getPlugin() {
        return plugin;
    }
    
    public static int getExperience(UUID playerId, SkillType skill, String itemKey) {
        return plugin.getSkillManager().getExperience(playerId, skill, itemKey);
    }
    
    public static void setExperience(UUID playerId, SkillType skill, String itemKey, int amount) {
        plugin.getSkillManager().getSkillData(playerId, skill).setExperience(itemKey, amount);
    }
    
    public static void addExperience(UUID playerId, SkillType skill, String itemKey, int amount) {
        int maxXP = plugin.getSkillConfig().getConfig(skill).maxExperience;
        plugin.getSkillManager().addExperience(playerId, skill, itemKey, amount, maxXP);
    }
    
    public static double getSuccessRate(UUID playerId, SkillType skill, String itemKey) {
        return plugin.getSkillManager().getSuccessRate(
            playerId,
            skill,
            itemKey,
            plugin.getSkillConfig().getConfig(skill).baseSuccessRate,
            plugin.getSkillConfig().getConfig(skill).maxSuccessRate,
            plugin.getSkillConfig().getConfig(skill).experiencePerLevel
        );
    }
    
    public static void registerHandler(SkillHandler handler) {
        plugin.getHandlerRegistry().registerExternal(handler);
    }
    
    public static void openSkillGUI(Player player) {
        plugin.getSkillGUI().openMainMenu(player);
    }
    
    public static void openSkillDetails(Player player, SkillType skill) {
        plugin.getSkillGUI().openSkillDetails(player, skill);
    }
    
    public static void savePlayerData(UUID playerId) {
        plugin.getDataManager().savePlayerData(playerId);
    }
    
    public static void saveAllData() {
        plugin.getDataManager().saveAllData();
    }
    
    public static void reloadConfig() {
        plugin.reloadConfiguration();
    }

    public static int getRepetitionCount(UUID playerId, SkillType skill, String itemKey) {
        return plugin.getSkillManager().getSkillData(playerId, skill).getRepetitionCount(itemKey);
    }

    public static void setRepetitionCount(UUID playerId, SkillType skill, String itemKey, int count) {
        plugin.getSkillManager().getSkillData(playerId, skill).setRepetitionCount(itemKey, count);
    }

    public static long getLastActionTime(UUID playerId, SkillType skill, String itemKey) {
        return plugin.getSkillManager().getSkillData(playerId, skill).getLastActionTime(itemKey);
    }

    public static void setLastActionTime(UUID playerId, SkillType skill, String itemKey, long time) {
        plugin.getSkillManager().getSkillData(playerId, skill).setLastActionTime(itemKey, time);
    }

    public static void applyDecay(UUID playerId, SkillType skill, String itemKey) {
        if (plugin.getSkillConfig().getDecay() == null || !plugin.getSkillConfig().getDecay().enabled) {
            return;
        }
        var decay = plugin.getSkillConfig().getDecay();
        plugin.getSkillManager().getSkillData(playerId, skill).applyDecay(
            itemKey,
            decay.initialHalfLifeHours,
            decay.stabilityIncreasePerRepetition,
            decay.maxStabilityMultiplier,
            decay.minRetentionPercentage
        );
    }

    public static boolean isDecayEnabled() {
        return plugin.getSkillConfig().getDecay() != null && plugin.getSkillConfig().getDecay().enabled;
    }

    public static double calculateLearningBoost(Player player, SkillType skill, String itemKey) {
        return plugin.getSkillManager().calculateBoost(player, skill, itemKey);
    }

    public static void addExperienceWithBoost(Player player, SkillType skill, String itemKey, int amount) {
        int maxXP = plugin.getSkillConfig().getConfig(skill).maxExperience;
        plugin.getSkillManager().addExperienceWithBoost(player, skill, itemKey, amount, maxXP);
    }

    public static ItemStack createSkillBook(SkillType skill, String itemKey, String authorName, int authorExperience) {
        return plugin.getBookManager().createSkillBook(skill, itemKey, authorName, authorExperience);
    }

    public static boolean isSkillBook(ItemStack item) {
        return plugin.getBookManager().isSkillBook(item);
    }

    public static SkillType getSkillBookType(ItemStack item) {
        return plugin.getBookManager().getSkillType(item);
    }

    public static String getSkillBookItemKey(ItemStack item) {
        return plugin.getBookManager().getItemKey(item);
    }

    public static List<SynergyCalculator.SynergyGrant> calculateSynergies(UUID playerId, SkillType skill, String itemKey, int baseXP) {
        return plugin.getSynergyCalculator().calculateSynergies(playerId, skill, itemKey, baseXP);
    }

    public static boolean isSynergyEnabled() {
        return plugin.getSynergyConfig() != null && plugin.getSynergyConfig().isEnabled();
    }

    public static boolean consumeHunger(Player player, HungerHandler.ActionType actionType) {
        return plugin.getHungerHandler().consumeHunger(player, actionType);
    }

    public static boolean consumeHunger(Player player, HungerHandler.ActionType actionType, int multiplier) {
        return plugin.getHungerHandler().consumeHunger(player, actionType, multiplier);
    }

    public static boolean consumeHungerRaw(Player player, double hungerCost) {
        return plugin.getHungerHandler().consumeHungerRaw(player, hungerCost);
    }
}
