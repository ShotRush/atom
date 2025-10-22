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

public final class GameplayFeatures {
    
    private final NamespacedKey reinforcedKey;
    private final NamespacedKey xpAmountKey;
    private final NamespacedKey skillIdKey;
    private final double extractionEfficiency = 0.8;
    
    public GameplayFeatures(Plugin plugin) {
        this.reinforcedKey = new NamespacedKey(plugin, "reinforced");
        this.xpAmountKey = new NamespacedKey(plugin, "xp_amount");
        this.skillIdKey = new NamespacedKey(plugin, "skill_id");
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
    

    public ItemStack createXpBook(String skillId, long xpAmount) {
        long storedXp = (long) (xpAmount * extractionEfficiency);
        
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        
        meta.getPersistentDataContainer().set(xpAmountKey, PersistentDataType.LONG, storedXp);
        meta.getPersistentDataContainer().set(skillIdKey, PersistentDataType.STRING, skillId);
        
        meta.displayName(Component.text("XP Tome: ", NamedTextColor.LIGHT_PURPLE)
            .append(Component.text(capitalize(skillId), NamedTextColor.GOLD)));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Stored XP: ", NamedTextColor.GRAY)
            .append(Component.text(storedXp, NamedTextColor.GREEN)));
        lore.add(Component.text("Skill: ", NamedTextColor.GRAY)
            .append(Component.text(skillId, NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        lore.add(Component.text("Right-click to absorb", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        
        book.setItemMeta(meta);
        return book;
    }
    
    public ItemStack createXpBottle(String skillId, long xpAmount) {
        long storedXp = (long) (xpAmount * extractionEfficiency);
        
        ItemStack bottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = bottle.getItemMeta();
        
        meta.getPersistentDataContainer().set(xpAmountKey, PersistentDataType.LONG, storedXp);
        meta.getPersistentDataContainer().set(skillIdKey, PersistentDataType.STRING, skillId);
        
        meta.displayName(Component.text("XP Essence: ", NamedTextColor.AQUA)
            .append(Component.text(capitalize(skillId), NamedTextColor.GOLD)));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Stored XP: ", NamedTextColor.GRAY)
            .append(Component.text(storedXp, NamedTextColor.GREEN)));
        lore.add(Component.text("Skill: ", NamedTextColor.GRAY)
            .append(Component.text(skillId, NamedTextColor.YELLOW)));
        meta.lore(lore);
        
        bottle.setItemMeta(meta);
        return bottle;
    }
    
    public boolean isXpItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(xpAmountKey, PersistentDataType.LONG) &&
               meta.getPersistentDataContainer().has(skillIdKey, PersistentDataType.STRING);
    }
    
    public long getStoredXp(ItemStack item) {
        if (!isXpItem(item)) return 0;
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(xpAmountKey, PersistentDataType.LONG, 0L);
    }
    
    public String getSkillId(ItemStack item) {
        if (!isXpItem(item)) return null;
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(skillIdKey, PersistentDataType.STRING);
    }
    
    public double getExtractionEfficiency() {
        return extractionEfficiency;
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
