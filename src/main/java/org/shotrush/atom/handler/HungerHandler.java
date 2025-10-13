package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.shotrush.atom.Atom;
import org.shotrush.atom.api.AtomAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HungerHandler implements Listener {
    private final Atom plugin;
    private final Map<UUID, Long> lastActionTime = new HashMap<>();
    private final Map<UUID, Double> accumulatedHunger = new HashMap<>();
    private static final long ACTION_COOLDOWN_MS = 50;
    private static final double HUNGER_THRESHOLD = 1.0;

    public HungerHandler(Atom plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public boolean consumeHunger(Player player, ActionType actionType) {
        return consumeHunger(player, actionType, 1);
    }

    public boolean consumeHunger(Player player, ActionType actionType, int multiplier) {
        double hungerCost = getHungerCost(actionType) * multiplier;
        return consumeHungerRaw(player, hungerCost);
    }

    public boolean consumeHungerRaw(Player player, double hungerCost) {
        UUID playerId = player.getUniqueId();
        int currentFoodLevel = player.getFoodLevel();

        if (currentFoodLevel <= 0) {
            return false;
        }

        double accumulated = accumulatedHunger.getOrDefault(playerId, 0.0) + hungerCost;
        
        if (accumulated < HUNGER_THRESHOLD) {
            accumulatedHunger.put(playerId, accumulated);
            return true;
        }

        double toConsume = accumulated;
        accumulatedHunger.put(playerId, 0.0);

        float currentSaturation = player.getSaturation();

        if (currentSaturation > 0) {
            float newSaturation = (float) Math.max(0, currentSaturation - toConsume);
            player.setSaturation(newSaturation);

            if (newSaturation == 0 && toConsume > currentSaturation) {
                double remainingCost = toConsume - currentSaturation;
                double newFoodLevel = Math.max(0, currentFoodLevel - remainingCost);
                player.setFoodLevel((int) newFoodLevel);
            }
        } else {
            double newFoodLevel = Math.max(0, currentFoodLevel - toConsume);
            player.setFoodLevel((int) newFoodLevel);
        }

        return true;
    }

    private double getHungerCost(ActionType actionType) {
        double baseHungerCost = plugin.getSkillConfig().getHungerCost();
        var multipliers = plugin.getSkillConfig().getHungerMultipliers();
        
        if (multipliers == null || multipliers.isEmpty()) {
            return switch (actionType) {
                case MINING -> baseHungerCost;
                case CRAFTING -> baseHungerCost;
                case SMELTING -> baseHungerCost;
                case COMBAT -> baseHungerCost * 1.2;
                case FISHING -> baseHungerCost * 0.8;
                case ENCHANTING -> baseHungerCost * 1.5;
                case BREEDING -> baseHungerCost * 0.7;
                case REPAIRING -> baseHungerCost;
                case BREWING -> baseHungerCost;
                case FARMING -> baseHungerCost;
                case BLOCK_PLACE -> baseHungerCost * 0.2;
                case INTERACT -> baseHungerCost * 0.1;
            };
        }
        
        String key = actionType.name().toLowerCase().replace("_", "-");
        double multiplier = multipliers.getOrDefault(key, 1.0);
        return baseHungerCost * multiplier;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        if (!canPerformAction(player)) {
            return;
        }

        if (!consumeHunger(player, ActionType.BLOCK_PLACE)) {
            event.setCancelled(true);
            player.sendMessage("§cYou're too hungry to place blocks!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (event.getClickedBlock() == null) {
            return;
        }

        if (!event.getAction().name().contains("RIGHT_CLICK")) {
            return;
        }

        String blockName = event.getClickedBlock().getType().name();
        if (!isInteractableBlock(blockName)) {
            return;
        }

        if (!canPerformAction(player)) {
            return;
        }

        if (!consumeHunger(player, ActionType.INTERACT)) {
            event.setCancelled(true);
            player.sendMessage("§cYou're too hungry to interact!");
        }
    }

    private boolean canPerformAction(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastActionTime.get(playerId);

        if (lastTime != null && (currentTime - lastTime) < ACTION_COOLDOWN_MS) {
            return false;
        }

        lastActionTime.put(playerId, currentTime);
        return true;
    }

    private boolean isInteractableBlock(String blockName) {
        return blockName.contains("DOOR") ||
               blockName.contains("GATE") ||
               blockName.contains("BUTTON") ||
               blockName.contains("LEVER") ||
               blockName.contains("CHEST") ||
               blockName.contains("FURNACE") ||
               blockName.contains("CRAFTING_TABLE") ||
               blockName.contains("ANVIL") ||
               blockName.contains("ENCHANTING_TABLE") ||
               blockName.contains("BREWING_STAND");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastActionTime.remove(playerId);
        accumulatedHunger.remove(playerId);
    }

    public enum ActionType {
        MINING,
        CRAFTING,
        SMELTING,
        COMBAT,
        FISHING,
        ENCHANTING,
        BREEDING,
        REPAIRING,
        BREWING,
        FARMING,
        BLOCK_PLACE,
        INTERACT
    }
}
