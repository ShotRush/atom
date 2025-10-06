package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillType;

public class MiningHandler extends SkillHandler implements Listener {
    
    public MiningHandler(Atom plugin) {
        super(plugin);
    }
    
    @Override
    public SkillType getSkillType() {
        return SkillType.MINING;
    }
    
    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getSkillConfig().getConfig(SkillType.MINING).enabled) {
            return;
        }
        
        Block block = event.getBlock();
        Material type = block.getType();
        
        if (!isMineable(type)) {
            return;
        }
        
        Player player = event.getPlayer();
        String itemKey = normalizeKey(type.name());
        
        if (!consumeHunger(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou're too hungry to mine!");
            return;
        }
        
        if (!rollSuccess(player, itemKey)) {
            event.setDropItems(false);
            event.setExpToDrop(0);
            player.sendMessage("§cYou failed to mine the " + type.name().toLowerCase().replace("_", " ") + "!");
        } else {
            player.sendMessage("§aSuccessfully mined " + type.name().toLowerCase().replace("_", " ") + "!");
        }
        
        grantExperience(player, itemKey);
    }
    
    private boolean isMineable(Material material) {
        String name = material.name();
        return name.endsWith("_ORE") || 
               name.equals("STONE") || 
               name.equals("COBBLESTONE") ||
               name.equals("DEEPSLATE") ||
               name.contains("ANCIENT_DEBRIS") ||
               name.contains("COAL") ||
               name.contains("DIAMOND") ||
               name.contains("EMERALD") ||
               name.contains("GOLD") ||
               name.contains("IRON") ||
               name.contains("LAPIS") ||
               name.contains("REDSTONE") ||
               name.contains("QUARTZ");
    }
}
