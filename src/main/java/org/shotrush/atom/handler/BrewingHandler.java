package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BrewingHandler extends SkillHandler implements Listener {
    private final Map<UUID, Long> lastBrewTime = new HashMap<>();
    
    public BrewingHandler(Atom plugin) {
        super(plugin);
    }
    
    @Override
    public SkillType getSkillType() {
        return SkillType.BREWING;
    }
    
    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (!plugin.getSkillConfig().getConfig(SkillType.BREWING).enabled) {
            return;
        }
        
        BrewerInventory inventory = event.getContents();
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(inventory.getLocation().getWorld())) {
                double distance = player.getLocation().distance(inventory.getLocation());
                if (distance < nearestDistance && distance < 5.0) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }
        
        if (nearestPlayer == null) {
            return;
        }
        
        Player player = nearestPlayer;
        String itemKey = "BREWING";
        
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastBrewTime.get(player.getUniqueId());
        if (lastTime != null && currentTime - lastTime < 1000) {
            return;
        }
        lastBrewTime.put(player.getUniqueId(), currentTime);
        
        if (!consumeHunger(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou're too hungry to brew!");
            return;
        }
        
        if (!rollSuccess(player, itemKey)) {
            event.setCancelled(true);
            player.sendMessage("§cBrewing failed! Ingredients were consumed.");
        } else {
            player.sendMessage("§aSuccessfully brewed potion!");
        }
        
        grantExperience(player, itemKey);
    }
}
