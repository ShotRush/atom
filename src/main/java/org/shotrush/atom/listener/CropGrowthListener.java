package org.shotrush.atom.listener;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.EnvironmentalManager;

import java.util.Random;

public class CropGrowthListener implements Listener {
    private final Atom plugin;
    private final EnvironmentalManager environmentalManager;
    private final Random random;

    public CropGrowthListener(Atom plugin, EnvironmentalManager environmentalManager) {
        this.plugin = plugin;
        this.environmentalManager = environmentalManager;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        
        double growthMultiplier = environmentalManager.getCropGrowthMultiplier(block);
        
        if (growthMultiplier >= 1.0) {
            if (random.nextDouble() > (1.0 / growthMultiplier)) {
                return;
            }
        } else {
            if (random.nextDouble() > growthMultiplier) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
