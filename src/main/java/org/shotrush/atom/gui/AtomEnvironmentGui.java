package org.shotrush.atom.gui;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.EnvironmentalManager;

import java.util.ArrayList;
import java.util.List;

public class AtomEnvironmentGui {
    private final Atom plugin;

    public AtomEnvironmentGui(Atom plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        EnvironmentalManager envManager = plugin.getEnvironmentalManager();
        envManager.updatePlayerEnvironment(player);
        
        Biome currentBiome = player.getLocation().getBlock().getBiome();
        double envBonus = envManager.getEnvironmentalBonus(player);
        double foodBonus = envManager.getFoodSurplusBonus(player);
        double totalBonus = envBonus + foodBonus;
        
        GuiFramework gui = GuiFramework.builder()
            .title("Environmental Factors")
            .size(3)
            
            .item(4, createCurrentBiomeItem(currentBiome, envBonus))
            .item(10, createFoodSurplusItem(foodBonus))
            .item(11, createTotalBonusItem(totalBonus))
            .item(12, createCircumscriptionItem())
            .item(13, createBiomeGuideItem())
            .item(14, createPopulationItem())
            .item(22, createBackItem())
            .build();

        gui.open(player);
    }

    private GuiFramework.GuiItem createCurrentBiomeItem(Biome biome, double bonus) {
        List<String> lore = new ArrayList<>();
        lore.add("Current Biome: " + formatBiomeName(biome));
        lore.add("Productivity: " + getBonusColor(bonus) + String.format("%+.0f%%", bonus * 100));
        lore.add("");
        lore.add("Your location affects how");
        lore.add("efficiently you can work!");
        
        return GuiFramework.GuiItem.builder()
            .material(getBiomeMaterial(biome))
            .name("Current Location")
            .lore(lore)
            
            .build();
    }

    private GuiFramework.GuiItem createFoodSurplusItem(double bonus) {
        List<String> lore = new ArrayList<>();
        lore.add("Food Surplus Bonus: " + getBonusColor(bonus) + String.format("%+.0f%%", bonus * 100));
        lore.add("");
        lore.add("Having excess food allows you");
        lore.add("to focus on other activities!");
        lore.add("");
        lore.add("Grow crops to increase surplus");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.BREAD)
            .name("Food Surplus")
            .lore(lore)
            .build();
    }

    private GuiFramework.GuiItem createTotalBonusItem(double totalBonus) {
        List<String> lore = new ArrayList<>();
        lore.add("Combined environmental effects");
        lore.add("");
        lore.add("Total Bonus: " + getBonusColor(totalBonus) + String.format("%+.0f%%", totalBonus * 100));
        lore.add("");
        
        if (totalBonus > 0.5) {
            lore.add("OPTIMAL CONDITIONS!");
        } else if (totalBonus > 0) {
            lore.add("Good conditions");
        } else if (totalBonus > -0.3) {
            lore.add("Challenging conditions");
        } else {
            lore.add("Harsh conditions");
        }
        
        return GuiFramework.GuiItem.builder()
            .material(Material.NETHER_STAR)
            .name("Total Environmental Bonus")
            .lore(lore)
            
            .build();
    }

    private GuiFramework.GuiItem createCircumscriptionItem() {
        List<String> lore = new ArrayList<>();
        lore.add("Resource scarcity and conflict");
        lore.add("can drive innovation!");
        lore.add("");
        lore.add("Low resources  Higher bonuses");
        lore.add("High conflict  Innovation boost");
        lore.add("");
        lore.add("Based on Carneiro's theory");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.IRON_SWORD)
            .name("Circumscription Theory")
            .lore(lore)
            .build();
    }

    private GuiFramework.GuiItem createBiomeGuideItem() {
        List<String> lore = new ArrayList<>();
        lore.add("Best Biomes:");
        lore.add("  Plains: +50%");
        lore.add("  Jungle: +40%");
        lore.add("  Forest: +30%");
        lore.add("");
        lore.add("Worst Biomes:");
        lore.add("  Desert: -40%");
        lore.add("  Mountains: -30%");
        lore.add("  Ocean: -20%");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.MAP)
            .name("Biome Productivity Guide")
            .lore(lore)
            .build();
    }

    private GuiFramework.GuiItem createPopulationItem() {
        List<String> lore = new ArrayList<>();
        lore.add("Population density affects");
        lore.add("resource availability");
        lore.add("");
        lore.add("High density  Resource scarcity");
        lore.add("Low density  More resources");
        lore.add("");
        lore.add("Malthusian pressure simulation");
        
        return GuiFramework.GuiItem.builder()
            .material(Material.PLAYER_HEAD)
            .name("Population Pressure")
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

    private String formatBiomeName(Biome biome) {
        String name = biome.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(Character.toUpperCase(word.charAt(0)))
                     .append(word.substring(1));
        }
        
        return formatted.toString();
    }

    private Material getBiomeMaterial(Biome biome) {
        switch (biome.name().toLowerCase()) {
            case "plains": return Material.GRASS_BLOCK;
            case "forest": return Material.OAK_LOG;
            case "jungle": return Material.JUNGLE_LOG;
            case "desert": return Material.SAND;
            case "mountains": return Material.STONE;
            case "ocean": return Material.WATER_BUCKET;
            case "swamp": return Material.LILY_PAD;
            case "taiga": return Material.SPRUCE_LOG;
            default: return Material.GRASS_BLOCK;
        }
    }

    private String getBonusColor(double bonus) {
        if (bonus >= 0.3) return "";
        if (bonus > 0) return "";
        if (bonus > -0.3) return "";
        return "";
    }
}
