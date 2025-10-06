package org.shotrush.atom.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GUIBuilder {
    private final Inventory inventory;
    
    public GUIBuilder(String title, int rows) {
        this.inventory = Bukkit.createInventory(null, rows * 9, title);
    }
    
    public GUIBuilder setItem(int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        
        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
        }
        
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
        return this;
    }
    
    public GUIBuilder setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
        return this;
    }
    
    public GUIBuilder fill(Material material) {
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
        return this;
    }
    
    public GUIBuilder border(Material material) {
        ItemStack border = new ItemStack(material);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        border.setItemMeta(meta);
        
        int size = inventory.getSize();
        int rows = size / 9;
        
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(size - 9 + i, border);
        }
        
        for (int i = 1; i < rows - 1; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
        
        return this;
    }
    
    public Inventory build() {
        return inventory;
    }
}
