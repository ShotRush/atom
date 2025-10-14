package org.shotrush.atom.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.PlayerData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AtomStatsGui {
    private final Atom plugin;
    private static final Map<UUID, Integer> playerPages = new HashMap<>();
    private static final int ITEMS_PER_PAGE = 28;

    public AtomStatsGui(Atom plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, playerPages.getOrDefault(player.getUniqueId(), 0));
    }

    public void open(Player player, int page) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        Map<String, Double> experience = data.getActionExperience();
        
        List<Map.Entry<String, Double>> sortedActions = experience.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .toList();
        
        int totalPages = (int) Math.ceil(sortedActions.size() / (double) ITEMS_PER_PAGE);
        final int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getUniqueId(), currentPage);
        
        GuiFramework.Builder builder = GuiFramework.builder()
            .title("Statistics (Page " + (currentPage + 1) + "/" + Math.max(1, totalPages) + ")")
            .size(5)
            ;
        
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, sortedActions.size());
        
        int slot = 10;
        
        for (int i = startIndex; i < endIndex; i++) {
            if (slot >= 35) break;
            if (slot % 9 == 0 || slot % 9 == 8) slot++;
            
            Map.Entry<String, Double> entry = sortedActions.get(i);
            
            double xp = entry.getValue();
            double efficiency = plugin.getEmergentBonusManager().getSpeedMultiplier(player, entry.getKey());
            double varietyBonus = plugin.getSkillTransferManager().getVarietyBonus(player);
            double masteryBonus = plugin.getSkillTransferManager().getMasteryBonus(player, entry.getKey());
            double socialBonus = plugin.getReputationManager().getReputationBonus(player);
            double envBonus = plugin.getEnvironmentalManager().getEnvironmentalBonus(player);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Experience §f" + String.format("%.0f", xp));
            lore.add("§7Efficiency §f" + String.format("%.2fx", efficiency));
            lore.add("");
            
            if (efficiency >= 2.5) lore.add("§f§lGRANDMASTER");
            else if (efficiency >= 2.0) lore.add("§f§lMASTER");
            else if (efficiency >= 1.5) lore.add("§f§lEXPERT");
            else if (efficiency >= 1.0) lore.add("§7SKILLED");
            else lore.add("§8LEARNING");
            lore.add("");
            
            boolean hasBonuses = varietyBonus > 0 || masteryBonus > 0 || socialBonus > 0 || envBonus > 0;
            if (hasBonuses) {
                lore.add("§7Bonuses");
                if (varietyBonus > 0) lore.add("§8• §fVariety §7+" + String.format("%.0f%%", varietyBonus * 100));
                if (masteryBonus > 0) lore.add("§8• §fMastery §7+" + String.format("%.0f%%", masteryBonus * 100));
                if (socialBonus > 0) lore.add("§8• §fSocial §7+" + String.format("%.0f%%", socialBonus * 100));
                if (envBonus > 0) lore.add("§8• §fEnvironment §7+" + String.format("%.0f%%", envBonus * 100));
                lore.add("");
            }
            
            lore.add("§7Benefits");
            lore.add("§8• §f" + String.format("%.0f%%", (efficiency - 1.0) * 100) + " §7faster");
            if (efficiency >= 2.5) lore.add("§8• §fAuto-Craft");
            if (efficiency >= 2.0) lore.add("§8• §fMaterial Refund");
            
            Material icon = getIconForAction(entry.getKey());
            
            builder.item(slot, GuiFramework.GuiItem.builder()
                .material(icon)
                .name(entry.getKey())
                .lore(lore)
                
                .build());
            
            slot++;
        }
        
        if (currentPage > 0) {
            builder.item(36, GuiFramework.GuiItem.builder()
                .material(Material.ARROW)
                .name("Previous Page")
                .lore("Go to page " + currentPage)
                .onClick(p -> open(p, currentPage - 1))
                .build());
        }
        
        builder.item(40, GuiFramework.GuiItem.builder()
            .material(Material.BARRIER)
            .name("Back")
            .lore("Return to main menu")
            .onClick(p -> new AtomMainGui(plugin).open(p))
            .build());
        
        if (currentPage < totalPages - 1) {
            builder.item(44, GuiFramework.GuiItem.builder()
                .material(Material.ARROW)
                .name("Next Page")
                .lore("Go to page " + (currentPage + 2))
                .onClick(p -> open(p, currentPage + 1))
                .build());
        }
        
        builder.build().open(player);
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
}
