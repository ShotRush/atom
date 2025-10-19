package org.shotrush.atom.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.shotrush.atom.manager.PlayerDataManager;

import java.util.Objects;

public final class PlayerConnectionListener implements Listener {
    
    private final PlayerDataManager dataManager;
    
    public PlayerConnectionListener(PlayerDataManager dataManager) {
        this.dataManager = Objects.requireNonNull(dataManager, "dataManager cannot be null");
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        player.getScheduler().run(
            player.getServer().getPluginManager().getPlugin("Atom"),
            task -> dataManager.loadPlayerData(player.getUniqueId()),
            null
        );
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        dataManager.savePlayerData(player.getUniqueId())
            .thenRun(() -> dataManager.unloadPlayerData(player.getUniqueId()));
    }
}
