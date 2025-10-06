package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillType;

public class RepairingHandler extends SkillHandler implements Listener {
    
    public RepairingHandler(Atom plugin) {
        super(plugin);
    }
    
    @Override
    public SkillType getSkillType() {
        return SkillType.REPAIRING;
    }
    
    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRepair(InventoryClickEvent event) {
        if (!plugin.getSkillConfig().getConfig(SkillType.REPAIRING).enabled) {
            return;
        }
        
        if (!(event.getInventory() instanceof AnvilInventory)) {
            return;
        }
        
        if (event.getSlotType() != InventoryType.SlotType.RESULT) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) {
            return;
        }
        
        String itemKey = normalizeKey(result.getType().name());
        
        if (!consumeHunger(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou're too hungry to repair!");
            return;
        }
        
        if (!rollSuccess(player, itemKey)) {
            event.setCancelled(true);
            player.sendMessage("§cRepair failed! Materials were consumed.");
        } else {
            player.sendMessage("§aSuccessfully repaired!");
        }
        
        grantExperience(player, itemKey);
    }
}
