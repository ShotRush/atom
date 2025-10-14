package org.shotrush.atom.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.PlayerDataManager;

public class PlayerListener implements Listener {
    private final Atom plugin;
    private final PlayerDataManager playerDataManager;

    public PlayerListener(Atom plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerDataManager.getPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataManager.unloadPlayerData(event.getPlayer().getUniqueId());
    }
}
