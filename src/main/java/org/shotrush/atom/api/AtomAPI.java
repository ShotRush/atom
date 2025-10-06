package org.shotrush.atom.api;

import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.handler.SkillHandler;
import org.shotrush.atom.skill.SkillType;

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
        plugin.reloadConfig();
        plugin.getSkillConfig().loadFromConfig(plugin.getConfig());
    }
}
