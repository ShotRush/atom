package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillType;

public class EnchantingHandler extends SkillHandler implements Listener {
    
    public EnchantingHandler(Atom plugin) {
        super(plugin);
    }
    
    @Override
    public SkillType getSkillType() {
        return SkillType.ENCHANTING;
    }
    
    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (!plugin.getSkillConfig().getConfig(SkillType.ENCHANTING).enabled) {
            return;
        }
        
        Player player = event.getEnchanter();
        String itemKey = "ENCHANTING";
        
        if (!consumeHunger(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou're too hungry to enchant!");
            return;
        }
        
        if (!rollSuccess(player, itemKey)) {
            consumeEnchantingMaterials(event, player);
            event.setCancelled(true);
            player.sendMessage("§cEnchanting failed! Lapis and levels were consumed.");
        } else {
            int exp = plugin.getSkillManager().getExperience(player.getUniqueId(), getSkillType(), itemKey);
            if (exp > 200 && random.nextDouble() < 0.3) {
                player.sendMessage("§aSuccessful enchantment with bonus!");
            } else {
                player.sendMessage("§aSuccessfully enchanted!");
            }
        }
        
        grantExperience(player, itemKey);
    }

    private void consumeEnchantingMaterials(EnchantItemEvent event, Player player) {
        int cost = event.getExpLevelCost();
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setLevel(Math.max(0, player.getLevel() - cost));
            
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.LAPIS_LAZULI) {
                    int lapisNeeded = Math.min(3, item.getAmount());
                    item.setAmount(item.getAmount() - lapisNeeded);
                    break;
                }
            }
        });
    }
}
