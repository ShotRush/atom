package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillType;

public class FarmingHandler extends SkillHandler implements Listener {
    
    public FarmingHandler(Atom plugin) {
        super(plugin);
    }
    
    @Override
    public SkillType getSkillType() {
        return SkillType.FARMING;
    }
    
    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        if (!plugin.getSkillConfig().getConfig(SkillType.FARMING).enabled) {
            return;
        }
        
        Block block = event.getBlock();
        BlockData data = block.getBlockData();
        
        if (!(data instanceof Ageable ageable)) {
            return;
        }
        
        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }
        
        Player player = event.getPlayer();
        String itemKey = normalizeKey(block.getType().name());
        
        if (!consumeHunger(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou're too hungry to harvest!");
            return;
        }
        
        if (!rollSuccess(player, itemKey)) {
            event.setDropItems(false);
            player.sendMessage("§cFailed to harvest the crop!");
        } else {
            player.sendMessage("§aSuccessfully harvested!");
        }
        
        grantExperience(player, itemKey);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBelowBreak(BlockBreakEvent event) {
        if (!plugin.getSkillConfig().getConfig(SkillType.FARMING).enabled) {
            return;
        }
        
        Block above = event.getBlock().getRelative(0, 1, 0);
        BlockData data = above.getBlockData();
        
        if (data instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge()) {
            Player player = event.getPlayer();
            String itemKey = normalizeKey(above.getType().name());
            
            if (!consumeHunger(player)) {
                event.setCancelled(true);
                player.sendMessage("§cYou're too hungry to harvest!");
                return;
            }
            
            if (!rollSuccess(player, itemKey)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    above.setType(Material.AIR);
                    above.getDrops().clear();
                });
                player.sendMessage("§cFailed to harvest the crop!");
            } else {
                player.sendMessage("§aSuccessfully harvested!");
            }
            
            grantExperience(player, itemKey);
        }
    }
}
