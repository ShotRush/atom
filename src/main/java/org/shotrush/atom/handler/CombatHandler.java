package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillType;

public class CombatHandler extends SkillHandler implements Listener {
    
    public CombatHandler(Atom plugin) {
        super(plugin);
    }
    
    @Override
    public SkillType getSkillType() {
        return SkillType.COMBAT;
    }
    
    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getSkillConfig().getConfig(SkillType.COMBAT).enabled) {
            return;
        }
        
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        
        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity)) {
            return;
        }
        
        String itemKey = normalizeKey(target.getType().name());
        
        if (!consumeHunger(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou're too hungry to fight!");
            return;
        }
        
        if (!rollSuccess(player, itemKey)) {
            event.setDamage(event.getDamage() * 0.3);
            player.sendMessage("§cYour attack was weak!");
        } else {
            int exp = plugin.getSkillManager().getExperience(player.getUniqueId(), getSkillType(), itemKey);
            double damageBonus = 1.0 + Math.min(exp / 1000.0, 1.0);
            event.setDamage(event.getDamage() * damageBonus);
            player.sendMessage("§aSuccessful hit!");
        }
        
        grantExperience(player, itemKey);
    }
}
