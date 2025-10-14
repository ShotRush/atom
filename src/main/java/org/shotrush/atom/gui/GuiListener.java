package org.shotrush.atom.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {
    
    public GuiListener() {
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            event.setCancelled(true);
            GuiHolder holder = (GuiHolder) event.getInventory().getHolder();
            holder.getGui().handleClick(event);
        }
    }
}
