package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
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
}
