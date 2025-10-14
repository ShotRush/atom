package org.shotrush.atom.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.PlayerData;

import java.util.*;

public class SkillGUI {
    private final Atom plugin;
    
    public SkillGUI(Atom plugin) {
        this.plugin = plugin;
    }
    
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Skill Progress");
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        Map<String, Double> experience = playerData.getActionExperience();
        
        List<Map.Entry<String, Double>> sortedActions = new ArrayList<>(experience.entrySet());
        sortedActions.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        int slot = 0;
        for (Map.Entry<String, Double> entry : sortedActions) {
            if (slot >= 27) break;
            
            ItemStack item = new ItemStack(getIconForAction(entry.getKey()));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(formatActionName(entry.getKey()));
            
            List<String> lore = new ArrayList<>();
            lore.add("Experience: " + String.format("%.1f", entry.getValue()));
            
            double efficiency = plugin.getEmergentBonusManager().getSpeedMultiplier(player, entry.getKey());
            lore.add("Efficiency: " + String.format("%.2fx", efficiency));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            
            inv.setItem(slot++, item);
        }
        
        player.openInventory(inv);
    }
    
    private Material getIconForAction(String actionId) {
        String lower = actionId.toLowerCase();
        if (lower.contains("mine") || lower.contains("dig")) return Material.IRON_PICKAXE;
        if (lower.contains("stone")) return Material.STONE;
        if (lower.contains("ore") || lower.contains("iron")) return Material.IRON_ORE;
        if (lower.contains("diamond")) return Material.DIAMOND;
        if (lower.contains("chop") || lower.contains("wood") || lower.contains("log")) return Material.IRON_AXE;
        if (lower.contains("harvest") || lower.contains("farm")) return Material.WHEAT;
        if (lower.contains("crop")) return Material.IRON_HOE;
        if (lower.contains("kill") || lower.contains("combat") || lower.contains("hunt")) return Material.IRON_SWORD;
        if (lower.contains("craft")) return Material.CRAFTING_TABLE;
        if (lower.contains("smelt")) return Material.FURNACE;
        if (lower.contains("brew")) return Material.BREWING_STAND;
        if (lower.contains("enchant")) return Material.ENCHANTING_TABLE;
        if (lower.contains("fish")) return Material.FISHING_ROD;
        if (lower.contains("breed")) return Material.WHEAT_SEEDS;
        if (lower.contains("trade")) return Material.EMERALD;
        if (lower.contains("build") || lower.contains("place")) return Material.BRICKS;
        return Material.BOOK;
    }

    private String formatActionName(String actionId) {
        return actionId.replace("_", " ").substring(0, 1).toUpperCase() + 
               actionId.replace("_", " ").substring(1);
    }
}
