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
    private final org.shotrush.atom.advancement.AdvancementGenerator advancementGenerator;
    private final org.shotrush.atom.tree.Trees.Registry treeRegistry;
    
    public PlayerConnectionListener(PlayerDataManager dataManager, 
                                   org.shotrush.atom.advancement.AdvancementGenerator advancementGenerator,
                                   org.shotrush.atom.tree.Trees.Registry treeRegistry) {
        this.dataManager = Objects.requireNonNull(dataManager, "dataManager cannot be null");
        this.advancementGenerator = advancementGenerator;
        this.treeRegistry = treeRegistry;
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        player.getScheduler().run(
            player.getServer().getPluginManager().getPlugin("Atom"),
            task -> {
                dataManager.loadPlayerData(player.getUniqueId());
                
                advancementGenerator.loadPlayerAdvancements(player);
                
                dataManager.getCachedPlayerData(player.getUniqueId()).ifPresent(data -> {
                    for (var tree : treeRegistry.getAllTrees()) {
                        advancementGenerator.updatePlayerAdvancements(player, data, tree);
                    }
                });
            },
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
