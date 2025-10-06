package org.shotrush.atom.handler;

import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.config.SkillConfig;
import org.shotrush.atom.skill.SkillType;

import java.util.Random;

public abstract class SkillHandler {
    protected final Atom plugin;
    protected final Random random = new Random();
    
    public SkillHandler(Atom plugin) {
        this.plugin = plugin;
    }
    
    public abstract SkillType getSkillType();
    
    public abstract void register();
    
    protected boolean consumeHunger(Player player) {
        double hungerCost = plugin.getSkillConfig().getHungerCost();
        int currentFoodLevel = player.getFoodLevel();
        
        if (currentFoodLevel <= 0) {
            return false;
        }
        
        double newFoodLevel = Math.max(0, currentFoodLevel - hungerCost);
        player.setFoodLevel((int) newFoodLevel);
        return true;
    }
    
    protected boolean rollSuccess(Player player, String itemKey) {
        SkillConfig.SkillTypeConfig config = plugin.getSkillConfig().getConfig(getSkillType());
        double successRate = plugin.getSkillManager().getSuccessRate(
            player.getUniqueId(),
            getSkillType(),
            itemKey,
            config.baseSuccessRate,
            config.maxSuccessRate,
            config.experiencePerLevel
        );
        return random.nextDouble() < successRate;
    }
    
    protected void grantExperience(Player player, String itemKey) {
        SkillConfig.SkillTypeConfig config = plugin.getSkillConfig().getConfig(getSkillType());
        int currentExp = plugin.getSkillManager().getExperience(player.getUniqueId(), getSkillType(), itemKey);
        
        if (currentExp >= config.maxExperience) {
            return;
        }
        
        plugin.getSkillManager().addExperience(
            player.getUniqueId(),
            getSkillType(),
            itemKey,
            config.experiencePerAction,
            config.maxExperience
        );
        
        int newExp = plugin.getSkillManager().getExperience(player.getUniqueId(), getSkillType(), itemKey);
        if (newExp >= config.maxExperience) {
            player.sendMessage("§6§lMAX LEVEL! §eYou've mastered " + itemKey.toLowerCase().replace("_", " ") + "!");
        }
    }
    
    protected String normalizeKey(String input) {
        return input.toUpperCase().replaceAll("_ORE$", "").replaceAll("_PLANKS$", "_WOOD");
    }
}
