package org.shotrush.atom.handler;

import org.bukkit.Location;
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
        return consumeHunger(player, 1);
    }
    
    protected boolean consumeHunger(Player player, int multiplier) {
        HungerHandler.ActionType actionType = getActionTypeForSkill(getSkillType());
        return plugin.getHungerHandler().consumeHunger(player, actionType, multiplier);
    }

    private HungerHandler.ActionType getActionTypeForSkill(SkillType skillType) {
        return switch (skillType) {
            case MINING -> HungerHandler.ActionType.MINING;
            case CRAFTING -> HungerHandler.ActionType.CRAFTING;
            case SMELTING -> HungerHandler.ActionType.SMELTING;
            case COMBAT -> HungerHandler.ActionType.COMBAT;
            case FISHING -> HungerHandler.ActionType.FISHING;
            case ENCHANTING -> HungerHandler.ActionType.ENCHANTING;
            case BREEDING -> HungerHandler.ActionType.BREEDING;
            case REPAIRING -> HungerHandler.ActionType.REPAIRING;
            case BREWING -> HungerHandler.ActionType.BREWING;
            case FARMING -> HungerHandler.ActionType.FARMING;
        };
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
        
        plugin.getSkillManager().addExperienceWithBoost(
            player,
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

    protected Player getNearestPlayer(Location loc, double radius) {
        if(loc.getWorld() == null) return null;
        Player nearest = null;
        double best = radius * radius;
        for (Player p : loc.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(loc);
            if (d <= best) {
                best = d;
                nearest = p;
            }
        }
        return nearest;
    }
}
