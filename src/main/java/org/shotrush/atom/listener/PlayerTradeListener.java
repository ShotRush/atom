package org.shotrush.atom.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.ReputationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerTradeListener implements Listener {
    private final Atom plugin;
    private final ReputationManager reputationManager;
    private final Map<UUID, DroppedItemInfo> droppedItems = new HashMap<>();
    private final Map<String, Long> tradeCooldowns = new HashMap<>();
    
    public PlayerTradeListener(Atom plugin, ReputationManager reputationManager) {
        this.plugin = plugin;
        this.reputationManager = reputationManager;
    }
    
    private long getTradeCooldownMs() {
        int seconds = plugin.getConfig().getInt("social-systems.reputation.trade-cooldown-seconds", 5);
        return seconds * 1000L;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!reputationManager.isEnabled()) return;
        
        Player player = event.getPlayer();
        Item droppedItem = event.getItemDrop();
        
        droppedItems.put(droppedItem.getUniqueId(), 
            new DroppedItemInfo(player.getUniqueId(), System.currentTimeMillis()));
        
        droppedItems.entrySet().removeIf(entry -> 
            System.currentTimeMillis() - entry.getValue().timestamp > 120000
        );
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!reputationManager.isEnabled()) return;
        
        Player picker = (Player) event.getEntity();
        Item item = event.getItem();
        DroppedItemInfo dropInfo = droppedItems.get(item.getUniqueId());
        
        if (dropInfo == null) return;
        if (dropInfo.dropperUUID.equals(picker.getUniqueId())) return;
        
        Player dropper = plugin.getServer().getPlayer(dropInfo.dropperUUID);
        if (dropper == null) return;
        
        String cooldownKey = getCooldownKey(dropInfo.dropperUUID, picker.getUniqueId());
        Long lastTrade = tradeCooldowns.get(cooldownKey);
        long cooldownMs = getTradeCooldownMs();
        
        if (lastTrade != null && System.currentTimeMillis() - lastTrade < cooldownMs) return;
        
        recordTrade(dropper, picker, item.getItemStack());
        tradeCooldowns.put(cooldownKey, System.currentTimeMillis());
        tradeCooldowns.entrySet().removeIf(entry -> 
            System.currentTimeMillis() - entry.getValue() > cooldownMs
        );
        droppedItems.remove(item.getUniqueId());
    }
    
    private void recordTrade(Player giver, Player receiver, ItemStack item) {
        reputationManager.recordTrade(giver, receiver, item.getAmount());
        if (plugin.getConfig().getBoolean("social-systems.reputation.notify-trades", false)) {
            giver.sendActionBar(Component.text("Trade with " + receiver.getName()));
            receiver.sendActionBar(Component.text("Trade with " + giver.getName()));
        }
    }
    
    private String getCooldownKey(UUID player1, UUID player2) {
        return player1.compareTo(player2) < 0 ? 
            player1.toString() + ":" + player2.toString() :
            player2.toString() + ":" + player1.toString();
    }
    
    private static class DroppedItemInfo {
        final UUID dropperUUID;
        final long timestamp;
        
        DroppedItemInfo(UUID dropperUUID, long timestamp) {
            this.dropperUUID = dropperUUID;
            this.timestamp = timestamp;
        }
    }
}
