package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillType;

import java.util.HashMap;
import java.util.Map;

public class SmeltingHandler extends SkillHandler implements Listener {
    private final Map<Location, Long> lastSmeltTime = new HashMap<>();
    
    public SmeltingHandler(Atom plugin) {
        super(plugin);
    }
    
    @Override
    public SkillType getSkillType() {
        return SkillType.SMELTING;
    }
    
    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmelt(BlockCookEvent event) {
        if (!plugin.getSkillConfig().getConfig(SkillType.SMELTING).enabled) {
            return;
        }
        
        Block furnace = event.getBlock();
        Location loc = furnace.getLocation();
        
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastSmeltTime.get(loc);
        if (lastTime != null && currentTime - lastTime < 500) {
            return;
        }
        lastSmeltTime.put(loc, currentTime);
        
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(furnace.getWorld())) {
                double distance = player.getLocation().distance(loc);
                if (distance < nearestDistance && distance < 10.0) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }
        
        if (nearestPlayer == null) {
            return;
        }
        
        Player player = nearestPlayer;
        Material resultType = event.getResult().getType();
        String itemKey = normalizeKey(resultType.name());
        
        if (!consumeHunger(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou're too hungry to smelt!");
            return;
        }
        
        double successRoll = random.nextDouble();
        double successRate = plugin.getSkillManager().getSuccessRate(
            player.getUniqueId(),
            getSkillType(),
            itemKey,
            plugin.getSkillConfig().getConfig(SkillType.SMELTING).baseSuccessRate,
            plugin.getSkillConfig().getConfig(SkillType.SMELTING).maxSuccessRate,
            plugin.getSkillConfig().getConfig(SkillType.SMELTING).experiencePerLevel
        );
        
        if (successRoll >= successRate) {
            double failType = random.nextDouble();
            
            if (failType < 0.5) {
                event.setCancelled(true);
                player.sendMessage("§cSmelting failed! Item was destroyed.");
            } else {
                Material nuggetType = getNuggetVersion(resultType);
                if (nuggetType != null) {
                    event.setResult(new ItemStack(nuggetType));
                    player.sendMessage("§6Smelting produced nuggets instead of ingots!");
                } else {
                    event.setCancelled(true);
                    player.sendMessage("§cSmelting failed! Item was destroyed.");
                }
            }
        } else {
            player.sendMessage("§aSuccessfully smelted " + resultType.name().toLowerCase().replace("_", " ") + "!");
        }
        
        grantExperience(player, itemKey);
    }
    
    private Material getNuggetVersion(Material material) {
        return switch (material) {
            case IRON_INGOT -> Material.IRON_NUGGET;
            case GOLD_INGOT -> Material.GOLD_NUGGET;
            default -> null;
        };
    }
}
