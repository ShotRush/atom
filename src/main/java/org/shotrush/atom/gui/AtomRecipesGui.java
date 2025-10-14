package org.shotrush.atom.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.RecipeDiscoveryManager;
import org.shotrush.atom.model.PlayerData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AtomRecipesGui {
    private final Atom plugin;

    public AtomRecipesGui(Atom plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        Set<String> discoveredRecipes = data.getDiscoveredRecipes();
        
        GuiFramework.Builder guiBuilder = GuiFramework.builder()
            .title("Discovered Recipes")
            .size(3)
            ;
        
        guiBuilder.item(4, createProgressItem(discoveredRecipes.size()));
        
        Map<String, RecipeDiscoveryManager.CustomRecipe> allRecipes = 
            plugin.getRecipeDiscoveryManager().getCustomRecipes();
        
        int slot = 9;
        for (Map.Entry<String, RecipeDiscoveryManager.CustomRecipe> entry : allRecipes.entrySet()) {
            String recipeId = entry.getKey();
            RecipeDiscoveryManager.CustomRecipe recipe = entry.getValue();
            boolean discovered = discoveredRecipes.contains(recipeId);
            
            if (slot >= 27) break;
            
            guiBuilder.item(slot++, createRecipeItem(recipe, discovered));
        }
        
        guiBuilder.item(22, createBackItem());
        
        guiBuilder.build().open(player);
    }

    private GuiFramework.GuiItem createProgressItem(int discovered) {
        int total = 6;
        double percentage = (discovered / (double) total) * 100;
        
        List<String> lore = new ArrayList<>();
        lore.add("Discovered: " + discovered + "/" + total);
        lore.add("Progress: " + String.format("%.0f%%", percentage));
        lore.add("");
        lore.add("Discover recipes by crafting");
        lore.add("with high efficiency!");
        lore.add("");
        
        if (discovered == total) {
            lore.add("ALL RECIPES DISCOVERED!");
        } else {
            lore.add("Keep crafting to discover more!");
        }
        
        return GuiFramework.GuiItem.builder()
            .material(Material.KNOWLEDGE_BOOK)
            .name("Recipe Discovery Progress")
            .lore(lore)
            
            .build();
    }

    private GuiFramework.GuiItem createRecipeItem(RecipeDiscoveryManager.CustomRecipe recipe, boolean discovered) {
        List<String> lore = new ArrayList<>();
        
        if (discovered) {
            lore.add("DISCOVERED");
            lore.add("");
            lore.add(recipe.getDescription());
            lore.add("");
            lore.add("Required Efficiency: " + String.format("%.1fx", recipe.getRequiredEfficiency()));
            lore.add("");
            lore.add("Recipe unlocked in your");
            lore.add("recipe book!");
        } else {
            lore.add("LOCKED");
            lore.add("");
            lore.add(recipe.getDescription());
            lore.add("");
            lore.add("Required Efficiency: " + String.format("%.1fx", recipe.getRequiredEfficiency()));
            lore.add("");
            lore.add("Craft items to discover!");
        }
        
        return GuiFramework.GuiItem.builder()
            .material(discovered ? recipe.getResult().getType() : Material.BARRIER)
            .name(recipe.getName())
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
}
