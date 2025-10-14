package org.shotrush.atom.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.ConfigManager;
import org.shotrush.atom.manager.EfficiencyManager;
import org.shotrush.atom.model.EfficiencyBonus;
import org.shotrush.atom.model.TrackedAction;
import org.shotrush.atom.model.TriggerType;

import java.util.Collection;
import java.util.Random;

public class EfficiencyListener implements Listener {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final EfficiencyManager efficiencyManager;
    private final Random random;

    public EfficiencyListener(Atom plugin, ConfigManager configManager, EfficiencyManager efficiencyManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.efficiencyManager = efficiencyManager;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreakEfficiency(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.BLOCK_BREAK && trigger.matches(material)) {
                    EfficiencyBonus bonus = configManager.getEfficiencyBonus(action.getId());
                    if (bonus == null) continue;

                    if (bonus.hasDropMultiplier()) {
                        applyDropMultiplier(event, player, action.getId());
                    }

                    if (bonus.hasFortuneBonus()) {
                        applyFortuneBonus(event, player, action.getId());
                    }

                    if (bonus.hasReplantChance()) {
                        applyReplantChance(event, player, action.getId());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageEfficiency(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.ENTITY_DEATH) {
                    EfficiencyBonus bonus = configManager.getEfficiencyBonus(action.getId());
                    if (bonus == null) continue;

                    if (bonus.hasDamageMultiplier()) {
                        double multiplier = efficiencyManager.getDamageMultiplier(player, action.getId());
                        event.setDamage(event.getDamage() * multiplier);
                    }
                }
            }
        }
    }

    private void applyDropMultiplier(BlockBreakEvent event, Player player, String actionId) {
        double multiplier = efficiencyManager.getDropMultiplier(player, actionId);
        if (multiplier <= 1.0) return;

        Block block = event.getBlock();
        Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand());
        
        if (random.nextDouble() < (multiplier - 1.0)) {
            for (ItemStack drop : drops) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
            }
        }
    }

    private void applyFortuneBonus(BlockBreakEvent event, Player player, String actionId) {
        int fortuneLevel = efficiencyManager.getFortuneBonus(player, actionId);
        if (fortuneLevel <= 0) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;

        int currentFortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        if (fortuneLevel > currentFortune) {
            Block block = event.getBlock();
            Material blockType = block.getType();
            
            if (blockType.name().contains("ORE") || blockType == Material.COAL_ORE || 
                blockType == Material.DIAMOND_ORE || blockType == Material.EMERALD_ORE ||
                blockType == Material.LAPIS_ORE || blockType == Material.REDSTONE_ORE) {
                
                int bonusDrops = random.nextInt(fortuneLevel + 1);
                Collection<ItemStack> drops = block.getDrops(tool);
                
                for (int i = 0; i < bonusDrops; i++) {
                    for (ItemStack drop : drops) {
                        block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
                    }
                }
            }
        }
    }

    private void applyReplantChance(BlockBreakEvent event, Player player, String actionId) {
        double chance = efficiencyManager.getReplantChance(player, actionId);
        if (chance <= 0 || random.nextDouble() > chance) return;

        Block block = event.getBlock();
        Material blockType = block.getType();
        
        if (blockType == Material.WHEAT || blockType == Material.CARROTS || 
            blockType == Material.POTATOES || blockType == Material.BEETROOTS) {
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (block.getType() == Material.AIR || block.getType() == Material.FARMLAND) {
                    block.setType(blockType);
                }
            }, 1L);
        }
    }
}
