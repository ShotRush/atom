package org.shotrush.atom.listener;

import org.shotrush.atom.Atom;

import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MiscListener implements Listener {
    private final Atom plugin;
    private final Map<UUID, Long> lastMovementDrain = new HashMap<>();
    private final Map<UUID, Double> accumulatedDistance = new HashMap<>();

    public MiscListener(Atom plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerInteractEntity(PlayerInteractEntityEvent event) {
        var entity = event.getRightClicked();
        if (entity instanceof MushroomCow && !plugin.getSkillConfig().getMisc().enableMushroomStew) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        double distance = event.getFrom().distance(event.getTo());
        UUID playerId = player.getUniqueId();
        
        double accumulated = accumulatedDistance.getOrDefault(playerId, 0.0) + distance;
        accumulatedDistance.put(playerId, accumulated);

        long currentTime = System.currentTimeMillis();
        Long lastDrain = lastMovementDrain.get(playerId);
        
        if (lastDrain == null || currentTime - lastDrain >= 1000) {
            if (accumulated >= 1.0) {
                drainMovementHunger(player, accumulated);
                accumulatedDistance.put(playerId, 0.0);
                lastMovementDrain.put(playerId, currentTime);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        if (!event.isSprinting()) {
            return;
        }

        Player player = event.getPlayer();
        drainHunger(player, 0.1);
    }

    private void drainMovementHunger(Player player, double distance) {
        boolean isSprinting = player.isSprinting();
        boolean isSwimming = player.isSwimming();
        boolean isFlying = player.isFlying();
        boolean isInWater = player.getLocation().getBlock().isLiquid();

        if (isFlying) {
            return;
        }

        double hungerCost = 0;

        if (isSprinting) {
            hungerCost = distance * 0.1;
        } else if (isSwimming || isInWater) {
            hungerCost = distance * 0.05;
        } else {
            hungerCost = distance * 0.02;
        }

        if (hungerCost > 0) {
            drainHunger(player, hungerCost);
        }
    }

    private void drainHunger(Player player, double cost) {
        int currentFoodLevel = player.getFoodLevel();
        float currentSaturation = player.getSaturation();

        if (currentFoodLevel <= 0) {
            return;
        }

        if (currentSaturation > 0) {
            float newSaturation = (float) Math.max(0, currentSaturation - cost);
            player.setSaturation(newSaturation);

            if (newSaturation == 0 && cost > currentSaturation) {
                double remainingCost = cost - currentSaturation;
                double newFoodLevel = Math.max(0, currentFoodLevel - remainingCost);
                player.setFoodLevel((int) newFoodLevel);
            }
        } else {
            double newFoodLevel = Math.max(0, currentFoodLevel - cost);
            player.setFoodLevel((int) newFoodLevel);
        }
    }
}
