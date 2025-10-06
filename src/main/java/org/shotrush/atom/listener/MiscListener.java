package org.shotrush.atom.listener;

import org.shotrush.atom.Atom;

import org.bukkit.entity.MushroomCow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class MiscListener implements Listener {
    private final Atom plugin;

    public MiscListener(Atom plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerInteractEntity(PlayerInteractEntityEvent event) {
        var entity = event.getRightClicked();
        if (entity instanceof MushroomCow) {
            if (!plugin.getSkillConfig().getMisc().enableMushroomStew) {
                event.setCancelled(true);
            }
        }
    }
}
