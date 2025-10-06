package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillType;

public class BreedingHandler extends SkillHandler implements Listener {
    
    public BreedingHandler(Atom plugin) {
        super(plugin);
    }
    
    @Override
    public SkillType getSkillType() {
        return SkillType.BREEDING;
    }
    
    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!plugin.getSkillConfig().getConfig(SkillType.BREEDING).enabled) {
            return;
        }
        
        if (!(event.getBreeder() instanceof Player player)) {
            return;
        }
        
        String itemKey = normalizeKey(event.getEntityType().name());
        
        if (!consumeHunger(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou're too hungry to breed animals!");
            return;
        }
        
        if (!rollSuccess(player, itemKey)) {
            event.setCancelled(true);
            player.sendMessage("§cBreeding failed! Food was consumed.");
        } else {
            player.sendMessage("§aSuccessfully bred " + event.getEntityType().name().toLowerCase() + "!");
        }
        
        grantExperience(player, itemKey);
    }
}
