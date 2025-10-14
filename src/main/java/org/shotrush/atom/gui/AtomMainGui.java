package org.shotrush.atom.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.PoliticalManager;
import org.shotrush.atom.manager.ReputationManager;
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
            .title("Atom")
            .size(3)
            
            .item(10, createStatsItem(player))
            .item(11, createSpecializationsItem(player))
            .item(12, createSocialItem(player))
            .item(13, createRecipesItem(player))
            .item(14, createEnvironmentItem(player))
            .item(15, createLeaderboardItem(player))
            .item(16, createPoliticalItem(player))
            .item(22, createCloseItem())
            .build();

        gui.open(player);
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
                lore.add("§8• §f" + entry.getKey() + " §7" + 
                    String.format("%.0f", entry.getValue()) + " §8(" + String.format("%.1fx", efficiency) + "§8");
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

    private GuiFramework.GuiItem createSocialItem(Player player) {
        ReputationManager repManager = plugin.getReputationManager();
        
        double socialCapital = repManager.getSocialCapital(player);
        int tradingPartners = repManager.getTradingPartners(player).size();
        double reputationBonus = repManager.getReputationBonus(player);
        
        List<String> lore = new ArrayList<>();
        lore.add("Social Capital: " + String.format("%.1f", socialCapital));
        lore.add("Trading Partners: " + tradingPartners);
        lore.add("Reputation Bonus: +" + String.format("%.0f%%", reputationBonus * 100));
        lore.add("");
        lore.add("Build trade networks to gain");
        lore.add("social capital and bonuses!");
        lore.add("");
        lore.add("Click to view social networks!");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.EMERALD)
            .name("Social Capital")
            .lore(lore)
            .onClick(p -> new AtomSocialGui(plugin).open(p))
            .build();
    }

    private GuiFramework.GuiItem createEnvironmentItem(Player player) {
        plugin.getEnvironmentalManager().updatePlayerEnvironment(player);
        double envBonus = plugin.getEnvironmentalManager().getEnvironmentalBonus(player);
        double foodBonus = plugin.getEnvironmentalManager().getFoodSurplusBonus(player);
        
        List<String> lore = new ArrayList<>();
        lore.add("Environment Bonus: " + String.format("%+.0f%%", envBonus * 100));
        lore.add("Food Surplus Bonus: " + String.format("%+.0f%%", foodBonus * 100));
        lore.add("");
        lore.add("Your biome affects productivity!");
        lore.add("Plains: +50%");
        lore.add("Desert: -40%");
        lore.add("");
        lore.add("Click to view environmental details!");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.GRASS_BLOCK)
            .name("Environment")
            .lore(lore)
            .onClick(p -> new AtomEnvironmentGui(plugin).open(p))
            .build();
    }

    private GuiFramework.GuiItem createLeaderboardItem(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add("View top players by:");
        lore.add("Total Experience");
        lore.add("Specialization Level");
        lore.add("Social Capital");
        lore.add("");
        lore.add("Click to view leaderboards!");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.DIAMOND)
            .name("Leaderboards")
            .lore(lore)
            .onClick(p -> new AtomLeaderboardGui(plugin).open(p))
            .build();
    }

    private GuiFramework.GuiItem createPoliticalItem(Player player) {
        PoliticalManager polManager = plugin.getPoliticalManager();
        
        boolean isBigMan = polManager.isBigMan(player);
        PoliticalManager.AuthorityType authority = polManager.getAuthorityType(player);
        double legitimacy = polManager.getLegitimacy(player);
        
        List<String> lore = new ArrayList<>();
        
        if (isBigMan) {
            lore.add("Status: Big Man");
            lore.add("Authority: " + authority.name());
            lore.add("Legitimacy: " + String.format("%.0f%%", legitimacy * 100));
            lore.add("");
            lore.add("You are a recognized leader!");
        } else {
            lore.add("Status: Citizen");
            lore.add("");
            lore.add("Gain social capital and surplus");
            lore.add("to become a leader!");
        }
        
        lore.add("");
        lore.add("Click to view political system!");
        
        return GuiFramework.GuiItem.builder()
            .material(isBigMan ? Material.GOLDEN_HELMET : Material.IRON_HELMET)
            .name("Political Status")
            .lore(lore)
            
            .onClick(p -> new AtomPoliticalGui(plugin).open(p))
            .build();
    }

    private GuiFramework.GuiItem createCloseItem() {
        return GuiFramework.GuiItem.builder()
            .material(Material.BARRIER)
            .name("Close")
            .lore("Click to close this menu")
            .onClick(Player::closeInventory)
            .build();
    }
}
