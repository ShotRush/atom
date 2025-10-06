package org.shotrush.atom.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillType;

public class GUIListener implements Listener {
    private final Atom plugin;
    
    public GUIListener(Atom plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        
        if (!title.contains("Skill Progress") && !title.contains("Details")) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        String itemName = clicked.getItemMeta().getDisplayName();
        
        if (itemName.equals("§cBack")) {
            plugin.getSkillGUI().openMainMenu(player);
            return;
        }
        
        if (title.contains("Skill Progress")) {
            for (SkillType type : SkillType.values()) {
                if (itemName.contains(type.name())) {
                    plugin.getSkillGUI().openSkillDetails(player, type);
                    break;
                }
            }
        }
    }
}
