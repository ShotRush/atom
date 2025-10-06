package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillType;

public class FishingHandler extends SkillHandler implements Listener {
    
    public FishingHandler(Atom plugin) {
        super(plugin);
    }
    
    @Override
    public SkillType getSkillType() {
        return SkillType.FISHING;
    }
    
    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!plugin.getSkillConfig().getConfig(SkillType.FISHING).enabled) {
            return;
        }
        
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        
        Player player = event.getPlayer();
        String itemKey = "FISHING";
        
        if (!consumeHunger(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou're too hungry to fish!");
            return;
        }
        
        if (!rollSuccess(player, itemKey)) {
            event.setCancelled(true);
            player.sendMessage("§cThe fish got away!");
        } else {
            player.sendMessage("§aSuccessfully caught a fish!");
        }
        
        grantExperience(player, itemKey);
    }
}
