package org.shotrush.atom.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.ConfigManager;
import org.shotrush.atom.manager.EmergentBonusManager;
import org.shotrush.atom.model.TrackedAction;
import org.shotrush.atom.model.TriggerType;

public class CraftingFatigueListener implements Listener {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final EmergentBonusManager emergentBonusManager;
    
    public CraftingFatigueListener(Atom plugin, ConfigManager configManager, EmergentBonusManager emergentBonusManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.emergentBonusManager = emergentBonusManager;
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        String actionId = findCraftingAction();
        if (actionId == null) {
            actionId = "craft_general";
        }
        
        double efficiency = emergentBonusManager.getSpeedMultiplier(player, actionId);
        
        applyCraftingCosts(player, efficiency, event);
    }
    
    private void applyCraftingCosts(Player player, double efficiency, CraftItemEvent event) {
        int hungerCost = calculateHungerCost(efficiency);
        float exhaustionCost = calculateExhaustionCost(efficiency);
        int slownessDuration = calculateSlownessDuration(efficiency);
        
        if (hungerCost > 0) {
            int currentFood = player.getFoodLevel();
            int newFood = Math.max(0, currentFood - hungerCost);
            
            if (currentFood < hungerCost) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("Too hungry to craft"));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            
            player.setFoodLevel(newFood);
        }
        
        if (exhaustionCost > 0) {
            player.setExhaustion(player.getExhaustion() + exhaustionCost);
        }
        
        if (slownessDuration > 0 && efficiency < 2.0) {
            int amplifier = calculateSlownessAmplifier(efficiency);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                slownessDuration,
                amplifier,
                false,
                false,
                true
            ));
            
            if (efficiency < 1.0) {
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.MINING_FATIGUE,
                    slownessDuration / 2,
                    0,
                    false,
                    false,
                    true
                ));
            }
        }
        
    }
    
    private int calculateHungerCost(double efficiency) {
        if (efficiency >= 3.0) return 0;
        if (efficiency >= 2.5) return 2;
        if (efficiency >= 2.0) return 4;
        if (efficiency >= 1.5) return 6;
        if (efficiency >= 1.0) return 8;
        if (efficiency >= 0.5) return 10;
        return 12;
    }
    
    private float calculateExhaustionCost(double efficiency) {
        if (efficiency >= 2.5) return 0.0f;
        if (efficiency >= 2.0) return 1.0f;
        if (efficiency >= 1.5) return 2.0f;
        if (efficiency >= 1.0) return 4.0f;
        if (efficiency >= 0.5) return 6.0f;
        return 8.0f;
    }
    
    private int calculateSlownessDuration(double efficiency) {
        if (efficiency >= 2.0) return 0;
        if (efficiency >= 1.5) return 60;
        if (efficiency >= 1.0) return 100;
        if (efficiency >= 0.5) return 140;
        return 200;
    }
    
    private int calculateSlownessAmplifier(double efficiency) {
        if (efficiency >= 1.5) return 0;
        if (efficiency >= 1.0) return 1;
        if (efficiency >= 0.5) return 2;
        return 4;
    }
    
    private String findCraftingAction() {
        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.CRAFT_ITEM) {
                    return action.getId();
                }
            }
        }
        return null;
    }
}
