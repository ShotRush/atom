package org.shotrush.atom.features;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class ToolReinforcement {
    
    private final NamespacedKey reinforcedKey;
    
    public ToolReinforcement(Plugin plugin) {
        this.reinforcedKey = new NamespacedKey(plugin, "reinforced");
    }
    
    public boolean canReinforce(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        if (isReinforced(item)) return false;
        
        Material type = item.getType();
        return type == Material.STONE_PICKAXE || type == Material.STONE_AXE || 
               type == Material.STONE_SHOVEL || type == Material.STONE_HOE ||
               type == Material.STONE_SWORD;
    }
    
    public boolean isReinforced(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(reinforcedKey, PersistentDataType.BYTE);
    }
    
    public ItemStack reinforce(ItemStack stoneTool) {
        if (!canReinforce(stoneTool)) return stoneTool;
        
        ItemStack reinforced = stoneTool.clone();
        ItemMeta meta = reinforced.getItemMeta();
        
        meta.getPersistentDataContainer().set(reinforcedKey, PersistentDataType.BYTE, (byte) 1);
        
        meta.displayName(Component.text("Reinforced ", NamedTextColor.GRAY)
            .append(Component.text(getToolName(stoneTool.getType()), NamedTextColor.WHITE)));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Reinforced with iron", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("+50% Durability", NamedTextColor.GREEN));
        meta.lore(lore);
        
        reinforced.setItemMeta(meta);
        return reinforced;
    }
    
    public int getReinforcedDurability(ItemStack item) {
        if (!isReinforced(item)) {
            return item.getType().getMaxDurability();
        }
        
        return (int) (item.getType().getMaxDurability() * 1.5);
    }
    
    private String getToolName(Material type) {
        return switch (type) {
            case STONE_PICKAXE -> "Stone Pickaxe";
            case STONE_AXE -> "Stone Axe";
            case STONE_SHOVEL -> "Stone Shovel";
            case STONE_HOE -> "Stone Hoe";
            case STONE_SWORD -> "Stone Sword";
            default -> type.name();
        };
    }
}
