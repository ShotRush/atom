package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
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

    /**
     * Handle harvesting logic for sugar cane and cactus stacks.
     * @param player The player responsible for the break.
     * @param base The block that is part of the stack (either the directly broken one, or the first one above the supporting block).
     * @param type The material type (SUGAR_CANE or CACTUS).
     * @param directBreak True if the player directly broke a cane/cactus block; false if it was the supporting block below.
     * @param event The BlockBreakEvent if directBreak is true; otherwise null.
     */
    private void handleCaneCactusHarvest(Player player, Block base, Material type, boolean directBreak, BlockBreakEvent event) {
        // Determine the contiguous stack from this base upwards
        Block current = base;
        int count = 0;
        while (current.getType() == type) {
            count++;
            current = current.getRelative(0, 1, 0);
        }

        String itemKey = normalizeKey(type.name());

        // Hunger cost only when the player intentionally breaks cane/cactus
        if (directBreak) {
            if (!consumeHunger(player, count)) {
                if (event != null) {
                    event.setCancelled(true);
                }
                player.sendMessage("§cYou're too hungry to harvest!");
                return;
            }
        }

        boolean success = rollSuccess(player, itemKey);

        if (!success) {
            // Suppress all drops from the stack segments we control by setting them to air on next tick
            // If this was a direct break, also suppress the base event drops
            if (event != null) {
                event.setDropItems(false);
            }
            // Remove every segment from base upwards that we enumerated
            Block toClear = base;
            for (int i = 0; i < count; i++) {
                Block finalToClear = toClear;
                Bukkit.getScheduler().runTask(plugin, () -> finalToClear.setType(Material.AIR));
                toClear = toClear.getRelative(0, 1, 0);
            }
            player.sendMessage("§cFailed to harvest the crop!");
        } else {
            player.sendMessage("§aSuccessfully harvested!");
            // On success we let vanilla physics handle the rest:
            // - event continues; upper segments will pop naturally and drop.
        }

        // Grant experience regardless of success to match crop behavior
        grantExperience(player, itemKey);
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
        
        // Handle mature crops planted on the broken block
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
                });
                player.sendMessage("§cFailed to harvest the crop!");
            } else {
                player.sendMessage("§aSuccessfully harvested!");
            }
            
            grantExperience(player, itemKey);
            return;
        }
        
        // Handle sugar cane and cactus stacks when the supporting block is broken
        Material aboveType = above.getType();
        Material belowType = event.getBlock().getType();
        var isCaneOrCactus = aboveType == Material.SUGAR_CANE || aboveType == Material.CACTUS;
        var isBelowCaneOrCactus = belowType == Material.SUGAR_CANE || belowType == Material.CACTUS;
        if (isCaneOrCactus && !isBelowCaneOrCactus) {
            handleCaneCactusHarvest(event.getPlayer(), above, aboveType, true, event);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCaneCactusBreak(BlockBreakEvent event) {
        if (!plugin.getSkillConfig().getConfig(SkillType.FARMING).enabled) {
            return;
        }
        
        Material type = event.getBlock().getType();
        if (type != Material.SUGAR_CANE && type != Material.CACTUS) {
            return;
        }
        
        Player player = event.getPlayer();
        handleCaneCactusHarvest(player, event.getBlock(), type, true, event);
    }
    
    // Attribute natural sugar cane/cactus breaks to the nearest player (no hunger/roll changes)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysicsBreak(BlockPhysicsEvent event) {
        Material type = event.getBlock().getType();
        if (type != Material.SUGAR_CANE && type != Material.CACTUS) {
            return;
        }
        // After physics resolution, see if it actually broke
        Block target = event.getBlock();
        if (target.getType() == Material.AIR) {
            Player nearest = getNearestPlayer(target.getLocation(), 6.0);
            if (nearest != null && plugin.getSkillConfig().getConfig(SkillType.FARMING).enabled) {
                handleCaneCactusHarvest(nearest, event.getBlock(), type, false, null);
            }
        }
    }
}
