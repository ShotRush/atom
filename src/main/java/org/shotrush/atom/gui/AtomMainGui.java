package org.shotrush.atom.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.PlayerData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AtomMainGui {
    private final Atom plugin;

    public AtomMainGui(Atom plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        GuiFramework gui = GuiFramework.builder()
            .title("§8§lAtom")
            .size(3)
            
            .item(11, createStatsItem(player))
            .item(13, createSpecializationsItem(player))
            .item(15, createRecipesItem(player))
            .item(22, createCloseItem())
            
            .item(0, createBorderItem())
            .item(1, createBorderItem())
            .item(7, createBorderItem())
            .item(8, createBorderItem())
            .item(9, createBorderItem())
            .item(17, createBorderItem())
            .item(18, createBorderItem())
            .item(19, createBorderItem())
            .item(25, createBorderItem())
            .item(26, createBorderItem())
            .build();

        gui.open(player);
    }
    
    private GuiFramework.GuiItem createBorderItem() {
        return GuiFramework.GuiItem.builder()
            .material(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .build();
    }

    private GuiFramework.GuiItem createStatsItem(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        Map<String, Double> experience = data.getActionExperience();
        
        int totalActions = experience.size();
        double totalXP = experience.values().stream().mapToDouble(Double::doubleValue).sum();
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Actions §f" + totalActions);
        lore.add("§7Total XP §f" + String.format("%.0f", totalXP));
        lore.add("");
        lore.add("§7Top Skills");
        experience.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(3)
            .forEach(entry -> {
                double efficiency = plugin.getEmergentBonusManager().getSpeedMultiplier(player, entry.getKey());
                String progressBar = getProgressBar(efficiency);
                lore.add("§8• §f" + entry.getKey() + " §7" + 
                    String.format("%.1fx", efficiency) + " " + progressBar);
            });
        
        
        return GuiFramework.GuiItem.builder()
            .material(Material.BOOK)
            .name("§fYour Statistics")
            .lore(lore)
            .onClick(p -> new AtomStatsGui(plugin).open(p))
            .build();
    }

    private GuiFramework.GuiItem createSpecializationsItem(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        
        double varietyBonus = plugin.getSkillTransferManager().getVarietyBonus(player);
        
        List<String> lore = new ArrayList<>();
        lore.add("Variety Bonus: +" + String.format("%.0f%%", varietyBonus * 100));
        lore.add("");
        lore.add("Your specializations emerge from");
        lore.add("what you actually do in-game.");
        lore.add("");
        lore.add("Click to view all specializations!");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.ENCHANTED_BOOK)
            .name("Specializations")
            .lore(lore)
            
            .onClick(p -> new AtomSpecializationsGui(plugin).open(p))
            .build();
    }

    private GuiFramework.GuiItem createRecipesItem(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int discovered = data.getDiscoveredRecipes().size();
        int total = 6;
        
        List<String> lore = new ArrayList<>();
        lore.add("Discovered: " + discovered + "/" + total);
        lore.add("");
        lore.add("Unlock legendary recipes by");
        lore.add("achieving high crafting efficiency!");
        lore.add("");
        
        if (discovered > 0) {
            lore.add(discovered + " recipes unlocked!");
        } else {
            lore.add("No recipes discovered yet");
        }
        
        lore.add("");
        lore.add("Click to view recipes!");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.CRAFTING_TABLE)
            .name("Discovered Recipes")
            .lore(lore)
            
            .onClick(p -> new AtomRecipesGui(plugin).open(p))
            .build();
    }

    private GuiFramework.GuiItem createCloseItem() {
        return GuiFramework.GuiItem.builder()
            .material(Material.BARRIER)
            .name("§cClose")
            .lore("§7Click to close this menu")
            .onClick(Player::closeInventory)
            .build();
    }
    
    private String getProgressBar(double efficiency) {
        double[] milestones = {1.0, 1.5, 2.0, 2.5};
        for (int i = 0; i < milestones.length - 1; i++) {
            if (efficiency >= milestones[i] && efficiency < milestones[i + 1]) {
                double progress = (efficiency - milestones[i]) / (milestones[i + 1] - milestones[i]);
                int filled = (int) (progress * 10);
                StringBuilder bar = new StringBuilder("§8[");
                for (int j = 0; j < 10; j++) {
                    bar.append(j < filled ? "§a■" : "§7■");
                }
                bar.append("§8]");
                return bar.toString();
            }
        }
        return "§8[§a■■■■■■■■■■§8]"; // Max level
    }
}
