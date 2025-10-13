package org.shotrush.atom.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.PlayerSkillData;
import org.shotrush.atom.skill.SkillData;
import org.shotrush.atom.skill.SkillMetadata;
import org.shotrush.atom.skill.SkillType;

import java.util.*;
import java.util.regex.Pattern;

public class SkillGUI {
    private final Atom plugin;
    private static final Map<String, Pattern> ITEM_GROUPS = new HashMap<>();
    
    static {
        ITEM_GROUPS.put("ORES", Pattern.compile(".*_(ORE|INGOT|NUGGET)$"));
        ITEM_GROUPS.put("WOOD", Pattern.compile(".*(WOOD|LOG|PLANKS)$"));
        ITEM_GROUPS.put("TOOLS", Pattern.compile(".*_(SWORD|PICKAXE|AXE|SHOVEL|HOE)$"));
        ITEM_GROUPS.put("ARMOR", Pattern.compile(".*_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)$"));
        ITEM_GROUPS.put("FOOD", Pattern.compile(".*(BREAD|MEAT|FISH|APPLE|CARROT|POTATO|BEETROOT)$"));
        ITEM_GROUPS.put("CROPS", Pattern.compile(".*(WHEAT|CARROT|POTATO|BEETROOT|MELON|PUMPKIN)$"));
        ITEM_GROUPS.put("ANIMALS", Pattern.compile(".*(COW|PIG|SHEEP|CHICKEN|RABBIT|HORSE)$"));
    }
    
    public SkillGUI(Atom plugin) {
        this.plugin = plugin;
    }
    
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6§lSkill Progress");
        
        PlayerSkillData playerData = plugin.getSkillManager().getPlayerData(player.getUniqueId());
        
        int slot = 0;
        for (SkillType type : SkillType.values()) {
            ItemStack item = new ItemStack(SkillMetadata.getIcon(type));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e§l" + type.name());
            
            SkillData skillData = playerData.getSkillData(type);
            Map<String, Integer> allExp = skillData.getAllExperience();
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to view details");
            lore.add("§7");
            lore.add("§6Total Items: §f" + allExp.size());
            
            int totalExp = allExp.values().stream().mapToInt(Integer::intValue).sum();
            lore.add("§6Total Experience: §f" + totalExp);
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            
            inv.setItem(slot++, item);
        }
        
        player.openInventory(inv);
    }
    
    public void openSkillDetails(Player player, SkillType skillType) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§l" + skillType.name() + " Details");
        
        PlayerSkillData playerData = plugin.getSkillManager().getPlayerData(player.getUniqueId());
        SkillData skillData = playerData.getSkillData(skillType);
        Map<String, Integer> allExp = skillData.getAllExperience();
        
        Map<String, Integer> groupedExp = groupItems(allExp);
        
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(groupedExp.entrySet());
        sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        int slot = 0;
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            if (slot >= 54) break;
            
            Material itemMaterial = getMaterialFromKey(entry.getKey());
            ItemStack item = new ItemStack(itemMaterial);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + formatItemName(entry.getKey()));
            
            List<String> lore = new ArrayList<>();
            lore.add("§6Experience: §f" + entry.getValue());
            
            double successRate = skillData.getSuccessRate(
                entry.getKey(),
                plugin.getSkillConfig().getConfig(skillType).baseSuccessRate,
                plugin.getSkillConfig().getConfig(skillType).maxSuccessRate,
                plugin.getSkillConfig().getConfig(skillType).experiencePerLevel
            );
            lore.add("§6Success Rate: §f" + String.format("%.1f%%", successRate * 100));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            
            inv.setItem(slot++, item);
        }
        
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§cBack");
        back.setItemMeta(backMeta);
        inv.setItem(53, back);
        
        player.openInventory(inv);
    }
    
    private Map<String, Integer> groupItems(Map<String, Integer> items) {
        Map<String, Integer> grouped = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String itemName = entry.getKey();
            String groupName = findGroup(itemName);
            
            if (groupName != null) {
                grouped.merge(groupName, entry.getValue(), Integer::sum);
            } else {
                grouped.put(itemName, entry.getValue());
            }
        }
        
        return grouped;
    }
    
    private String findGroup(String itemName) {
        for (Map.Entry<String, Pattern> group : ITEM_GROUPS.entrySet()) {
            if (group.getValue().matcher(itemName).matches()) {
                return group.getKey();
            }
        }
        return null;
    }
    
    public static void addItemGroup(String groupName, Pattern pattern) {
        ITEM_GROUPS.put(groupName, pattern);
    }
    
    public static void removeItemGroup(String groupName) {
        ITEM_GROUPS.remove(groupName);
    }

    private Material getMaterialFromKey(String key) {
        if (ITEM_GROUPS.containsKey(key)) {
            return getGroupIcon(key);
        }
        
        try {
            return Material.valueOf(key);
        } catch (IllegalArgumentException e) {
            return Material.PAPER;
        }
    }

    private Material getGroupIcon(String groupName) {
        return switch (groupName) {
            case "ORES" -> Material.IRON_ORE;
            case "WOOD" -> Material.OAK_LOG;
            case "TOOLS" -> Material.IRON_PICKAXE;
            case "ARMOR" -> Material.IRON_CHESTPLATE;
            case "FOOD" -> Material.BREAD;
            case "CROPS" -> Material.WHEAT;
            case "ANIMALS" -> Material.COW_SPAWN_EGG;
            default -> Material.PAPER;
        };
    }

    private String formatItemName(String key) {
        return key.toLowerCase().replace("_", " ");
    }
}
