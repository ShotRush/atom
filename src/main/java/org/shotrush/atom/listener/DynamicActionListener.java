package org.shotrush.atom.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.DynamicActionManager;

public class DynamicActionListener implements Listener {
    private final Atom plugin;
    private final DynamicActionManager dynamicActionManager;
    
    public DynamicActionListener(Atom plugin, DynamicActionManager dynamicActionManager) {
        this.plugin = plugin;
        this.dynamicActionManager = dynamicActionManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        dynamicActionManager.trackBlockBreak(event.getPlayer(), event);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        dynamicActionManager.trackBlockPlace(event.getPlayer(), event);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            dynamicActionManager.trackEntityKill(killer, event);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            dynamicActionManager.trackCrafting((Player) event.getWhoClicked(), event);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            dynamicActionManager.trackFishing(event.getPlayer(), event);
        }
    }
}
