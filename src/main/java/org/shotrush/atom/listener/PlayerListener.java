package org.shotrush.atom.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.shotrush.atom.Atom;

public class PlayerListener implements Listener {
    private final Atom plugin;
    
    public PlayerListener(Atom plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getSkillManager().getPlayerData(event.getPlayer().getUniqueId());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getDataManager().savePlayerData(event.getPlayer().getUniqueId());
    }
}
