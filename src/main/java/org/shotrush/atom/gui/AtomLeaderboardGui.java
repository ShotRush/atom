package org.shotrush.atom.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.PlayerData;

import java.util.*;
import java.util.stream.Collectors;

public class AtomLeaderboardGui {
    private final Atom plugin;
    private LeaderboardType currentType;

    public AtomLeaderboardGui(Atom plugin) {
        this.plugin = plugin;
        this.currentType = LeaderboardType.TOTAL_XP;
    }

    public void open(Player player) {
        open(player, currentType);
    }

    public void open(Player player, LeaderboardType type) {
        this.currentType = type;
        
        GuiFramework.Builder guiBuilder = GuiFramework.builder()
            .title("Leaderboards - " + type.displayName)
            .size(3)
            ;
        
        guiBuilder.item(3, createTypeButton(LeaderboardType.TOTAL_XP, player));
        guiBuilder.item(5, createTypeButton(LeaderboardType.SPECIALIZATION, player));
        
        List<LeaderboardEntry> entries = getLeaderboardData(type);
        
        int slot = 9;
        for (int i = 0; i < Math.min(entries.size(), 18); i++) {
            LeaderboardEntry entry = entries.get(i);
            
            if (slot >= 27) break;
            
            guiBuilder.item(slot++, createLeaderboardItem(entry, i + 1, player));
        }
        
        guiBuilder.item(22, createBackItem());
        
        guiBuilder.build().open(player);
    }

    private GuiFramework.GuiItem createTypeButton(LeaderboardType type, Player player) {
        boolean selected = type == currentType;
        
        List<String> lore = new ArrayList<>();
        lore.add("" + type.description);
        lore.add("");
        
        if (selected) {
            lore.add("SELECTED");
        } else {
            lore.add("Click to view!");
        }
        
        return GuiFramework.GuiItem.builder()
            .material(type.material)
            .name((selected ? "" : "") + type.displayName)
            .lore(lore)
            
            .onClick(p -> open(p, type))
            .build();
    }

    private GuiFramework.GuiItem createLeaderboardItem(LeaderboardEntry entry, int rank, Player viewer) {
        boolean isViewer = entry.playerName.equals(viewer.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add("Rank: " + getRankColor(rank) + "#" + rank);
        lore.add("Value: " + String.format("%.1f", entry.value));
        lore.add("");
        
        if (isViewer) {
            lore.add("This is you!");
        }
        
        Material material;
        if (rank == 1) material = Material.GOLD_BLOCK;
        else if (rank == 2) material = Material.IRON_BLOCK;
        else if (rank == 3) material = Material.COPPER_BLOCK;
        else material = Material.PLAYER_HEAD;
        
        return GuiFramework.GuiItem.builder()
            .material(material)
            .name((isViewer ? "" : "") + entry.playerName)
            .lore(lore)
            
            .build();
    }

    private GuiFramework.GuiItem createBackItem() {
        return GuiFramework.GuiItem.builder()
            .material(Material.ARROW)
            .name("Back to Main Menu")
            .lore("Click to go back")
            .onClick(p -> new AtomMainGui(plugin).open(p))
            .build();
    }

    private List<LeaderboardEntry> getLeaderboardData(LeaderboardType type) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(onlinePlayer.getUniqueId());
            double value = 0;
            
            switch (type) {
                case TOTAL_XP:
                    value = data.getActionExperience().values().stream()
                        .mapToDouble(Double::doubleValue).sum();
                    break;
                    
                case SPECIALIZATION:
                    value = data.getActionExperience().values().stream()
                        .mapToDouble(Double::doubleValue)
                        .max().orElse(0);
                    break;
            }
            
            entries.add(new LeaderboardEntry(onlinePlayer.getName(), value));
        }
        
        entries.sort((a, b) -> Double.compare(b.value, a.value));
        return entries;
    }

    private String getRankColor(int rank) {
        if (rank == 1) return "";
        if (rank == 2) return "";
        if (rank == 3) return "";
        return "";
    }

    private static class LeaderboardEntry {
        final String playerName;
        final double value;
        
        LeaderboardEntry(String playerName, double value) {
            this.playerName = playerName;
            this.value = value;
        }
    }

    private enum LeaderboardType {
        TOTAL_XP("Total Experience", "Sum of all action XP", Material.BOOK),
        SPECIALIZATION("Top Specialization", "Highest XP in a single action", Material.ENCHANTED_BOOK);
        
        final String displayName;
        final String description;
        final Material material;
        
        LeaderboardType(String displayName, String description, Material material) {
            this.displayName = displayName;
            this.description = description;
            this.material = material;
        }
    }
}
