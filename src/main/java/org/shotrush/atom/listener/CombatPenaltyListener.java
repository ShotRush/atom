package org.shotrush.atom.listener;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.ConfigManager;
import org.shotrush.atom.manager.EmergentBonusManager;
import org.shotrush.atom.model.TrackedAction;
import org.shotrush.atom.model.TriggerType;

public class CombatPenaltyListener implements Listener {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final EmergentBonusManager emergentBonusManager;
    
    public CombatPenaltyListener(Atom plugin, ConfigManager configManager, EmergentBonusManager emergentBonusManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.emergentBonusManager = emergentBonusManager;
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        
        String actionId = findCombatAction();
        if (actionId == null) {
            actionId = "kill_general";
        }
        
        double efficiency = emergentBonusManager.getSpeedMultiplier(player, actionId);
        
        applyDamagePenalty(event, efficiency);
        applyAttackCooldownPenalty(player, efficiency);
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToolDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        
        if (!event.getItem().getType().name().contains("SWORD") &&
            !event.getItem().getType().name().contains("AXE") &&
            !event.getItem().getType().name().contains("PICKAXE") &&
            !event.getItem().getType().name().contains("SHOVEL") &&
            !event.getItem().getType().name().contains("HOE")) {
            return;
        }
        
        String actionId = determineToolAction(event.getItem().getType().name());
        double efficiency = emergentBonusManager.getSpeedMultiplier(player, actionId);
        
        applyToolDurabilityPenalty(event, efficiency);
    }
    
    private void applyDamagePenalty(EntityDamageByEntityEvent event, double efficiency) {
        double damageMultiplier = calculateDamageMultiplier(efficiency);
        
        if (damageMultiplier < 1.0) {
            double originalDamage = event.getDamage();
            double newDamage = originalDamage * damageMultiplier;
            event.setDamage(newDamage);
        }
    }
    
    private void applyAttackCooldownPenalty(Player player, double efficiency) {
        if (efficiency >= 2.0) return;
        
        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed == null) return;
        
        double penalty = calculateAttackSpeedPenalty(efficiency);
        
        if (penalty != 0) {
            attackSpeed.getModifiers().stream()
                .filter(mod -> mod.getKey().getNamespace().equals("atom") && 
                              mod.getKey().getKey().contains("combat_penalty"))
                .toList()
                .forEach(attackSpeed::removeModifier);
            
            if (penalty < 0) {
                AttributeModifier modifier = new AttributeModifier(
                    new org.bukkit.NamespacedKey(plugin, "combat_penalty_" + player.getUniqueId()),
                    penalty,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                attackSpeed.addModifier(modifier);
            }
        }
    }
    
    private void applyToolDurabilityPenalty(PlayerItemDamageEvent event, double efficiency) {
        int originalDamage = event.getDamage();
        int penaltyMultiplier = calculateDurabilityPenalty(efficiency);
        
        if (penaltyMultiplier > 1) {
            event.setDamage(originalDamage * penaltyMultiplier);
        }
    }
    
    private double calculateDamageMultiplier(double efficiency) {
        if (efficiency >= 2.0) return 1.0;
        if (efficiency >= 1.5) return 0.95;
        if (efficiency >= 1.0) return 0.90;
        if (efficiency >= 0.5) return 0.80;
        return 0.70;
    }
    
    private double calculateAttackSpeedPenalty(double efficiency) {
        if (efficiency >= 2.0) return 0.0;
        if (efficiency >= 1.5) return -0.2;
        if (efficiency >= 1.0) return -0.4;
        if (efficiency >= 0.5) return -0.6;
        return -0.8;
    }
    
    private int calculateDurabilityPenalty(double efficiency) {
        if (efficiency >= 2.5) return 1;
        if (efficiency >= 2.0) return 1;
        if (efficiency >= 1.5) return 2;
        if (efficiency >= 1.0) return 3;
        if (efficiency >= 0.5) return 4;
        return 5;
    }
    
    private String determineToolAction(String toolType) {
        if (toolType.contains("SWORD") || toolType.contains("AXE")) {
            return findCombatAction();
        } else if (toolType.contains("PICKAXE")) {
            return "mine_stone";
        } else if (toolType.contains("SHOVEL")) {
            return "dig_general";
        } else if (toolType.contains("HOE")) {
            return "harvest_crops";
        }
        return "general";
    }
    
    private String findCombatAction() {
        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.ENTITY_DEATH || 
                    trigger.getType() == TriggerType.ENTITY_DAMAGE) {
                    return action.getId();
                }
            }
        }
        return null;
    }
}
