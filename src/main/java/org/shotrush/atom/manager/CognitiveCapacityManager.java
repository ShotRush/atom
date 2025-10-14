package org.shotrush.atom.manager;

import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.PlayerData;

import java.util.*;
import java.util.stream.Collectors;

public class CognitiveCapacityManager {
    private final Atom plugin;
    private final ConfigManager configManager;
    
    private boolean enabled;
    private int maxActiveSpecializations;
    private double proficiencyThreshold;
    private double masteryThreshold;
    private double interferencePenalty;
    private String calculationMode;

    public CognitiveCapacityManager(Atom plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        loadConfig();
    }

    public void loadConfig() {
        enabled = plugin.getConfig().getBoolean("specialization.cognitive-capacity.enabled", true);
        maxActiveSpecializations = plugin.getConfig().getInt("specialization.cognitive-capacity.max-active-specializations", 4);
        proficiencyThreshold = plugin.getConfig().getDouble("specialization.cognitive-capacity.proficiency-threshold", 1.0);
        masteryThreshold = plugin.getConfig().getDouble("specialization.cognitive-capacity.mastery-threshold", 1.5);
        interferencePenalty = plugin.getConfig().getDouble("specialization.cognitive-capacity.interference-penalty", 0.15);
        calculationMode = plugin.getConfig().getString("specialization.cognitive-capacity.calculation-mode", "slot-based");
    }

    public boolean isEnabled() {
        return enabled;
    }
    public double applyCognitiveLoad(Player player, String actionId, double baseEfficiency) {
        if (!enabled) return baseEfficiency;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        Map<String, Double> experience = data.getActionExperience();
        
        List<String> proficientSkills = experience.entrySet().stream()
            .filter(e -> {
                double exp = e.getValue();
                double efficiency = calculateDirectEfficiency(exp);
                return efficiency >= proficiencyThreshold && !actionId.equals(e.getKey());
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        int activeSpecializations = proficientSkills.size();
        
        if (activeSpecializations <= maxActiveSpecializations) {
            return baseEfficiency;
        }
        
        if ("slot-based".equals(calculationMode)) {
            return applySlotBasedPenalty(baseEfficiency, activeSpecializations);
        } else {
            return applyInterferencePenalty(baseEfficiency, activeSpecializations);
        }
    }
    
    private double calculateDirectEfficiency(double experience) {
        double baseEfficiency = configManager.getBaseEfficiency();
        double maxEfficiency = configManager.getMaxEfficiency();
        double expPerPoint = configManager.getExperiencePerEfficiencyPoint();
        
        double efficiency = baseEfficiency + (experience / expPerPoint);
        return Math.min(efficiency, maxEfficiency);
    }

    private double applySlotBasedPenalty(double baseEfficiency, int activeSpecializations) {
        int overflow = activeSpecializations - maxActiveSpecializations;
        double penalty = overflow * interferencePenalty;
        return Math.max(0.08, baseEfficiency * (1.0 - penalty));
    }

    private double applyInterferencePenalty(double baseEfficiency, int activeSpecializations) {
        double loadFactor = (double) activeSpecializations / maxActiveSpecializations;
        if (loadFactor <= 1.0) return baseEfficiency;
        
        double penalty = (loadFactor - 1.0) * interferencePenalty;
        return Math.max(0.08, baseEfficiency * (1.0 - penalty));
    }

    public int getActiveSpecializations(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        Map<String, Double> experience = data.getActionExperience();
        
        return (int) experience.entrySet().stream()
            .filter(e -> {
                double efficiency = calculateDirectEfficiency(e.getValue());
                return efficiency >= proficiencyThreshold;
            })
            .count();
    }

    public int getRemainingCapacity(Player player) {
        return Math.max(0, maxActiveSpecializations - getActiveSpecializations(player));
    }

    public List<String> getProficientSkills(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        Map<String, Double> experience = data.getActionExperience();
        
        return experience.entrySet().stream()
            .filter(e -> {
                double efficiency = calculateDirectEfficiency(e.getValue());
                return efficiency >= proficiencyThreshold;
            })
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public boolean isAtCapacity(Player player) {
        return getActiveSpecializations(player) >= maxActiveSpecializations;
    }

    public String getCapacityStatus(Player player) {
        int active = getActiveSpecializations(player);
        int max = maxActiveSpecializations;
        
        if (active < max) {
            return "§a" + active + "/" + max + " §7(Room for growth)";
        } else if (active == max) {
            return "§e" + active + "/" + max + " §7(At capacity)";
        } else {
            return "§c" + active + "/" + max + " §7(Overloaded! -" + ((active - max) * (int)(interferencePenalty * 100)) + "% efficiency)";
        }
    }

    public int getMaxActiveSpecializations() {
        return maxActiveSpecializations;
    }

    public double getProficiencyThreshold() {
        return proficiencyThreshold;
    }

    public double getMasteryThreshold() {
        return masteryThreshold;
    }
}
