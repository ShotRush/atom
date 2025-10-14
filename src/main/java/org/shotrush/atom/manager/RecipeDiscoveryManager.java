package org.shotrush.atom.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.PlayerData;

import java.util.*;

public class RecipeDiscoveryManager {
    private final Atom plugin;
    private final PlayerDataManager playerDataManager;
    private final EmergentBonusManager emergentBonusManager;
    private final Map<String, CustomRecipe> customRecipes;
    private final Random random;
    
    public RecipeDiscoveryManager(Atom plugin, PlayerDataManager playerDataManager, EmergentBonusManager emergentBonusManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.emergentBonusManager = emergentBonusManager;
        this.customRecipes = new HashMap<>();
        this.random = new Random();
        
        initializeCustomRecipes();
    }
    
    private void initializeCustomRecipes() {
        addRecipe("master_pickaxe", 3.0, createMasterPickaxe());
        addRecipe("master_sword", 3.0, createMasterSword());
        addRecipe("master_axe", 3.0, createMasterAxe());
        addRecipe("efficiency_charm", 2.5, createEfficiencyCharm());
        addRecipe("experience_condenser", 2.8, createExperienceCondenser());
        addRecipe("fortune_amulet", 3.2, createFortuneAmulet());
    }
    
    private void addRecipe(String id, double requiredEfficiency, CustomRecipe recipe) {
        recipe.setRequiredEfficiency(requiredEfficiency);
        customRecipes.put(id, recipe);
    }
    
    public void checkRecipeDiscovery(Player player, String craftingActionId) {
        double efficiency = emergentBonusManager.getSpeedMultiplier(player, craftingActionId);
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        
        for (Map.Entry<String, CustomRecipe> entry : customRecipes.entrySet()) {
            String recipeId = entry.getKey();
            CustomRecipe recipe = entry.getValue();
            
            if (data.hasDiscoveredRecipe(recipeId)) continue;
            if (efficiency < recipe.getRequiredEfficiency()) continue;
            
            double discoveryChance = calculateDiscoveryChance(efficiency, recipe.getRequiredEfficiency());
            
            if (random.nextDouble() < discoveryChance) {
                discoverRecipe(player, recipeId, recipe);
            }
        }
    }
    
    private double calculateDiscoveryChance(double currentEfficiency, double requiredEfficiency) {
        double excess = currentEfficiency - requiredEfficiency;
        return Math.min(0.01 + (excess * 0.02), 0.15);
    }
    
    private void discoverRecipe(Player player, String recipeId, CustomRecipe recipe) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        data.discoverRecipe(recipeId);
        
        recipe.registerRecipe(plugin, player);
        
        player.sendMessage(Component.text("════════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("    ✦ RECIPE DISCOVERED! ✦", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("  " + recipe.getName(), NamedTextColor.YELLOW, TextDecoration.ITALIC));
        player.sendMessage(Component.text("  " + recipe.getDescription(), NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("════════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
        
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
    }
    
    private CustomRecipe createMasterPickaxe() {
        ItemStack result = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = result.getItemMeta();
        meta.displayName(Component.text("Master's Pickaxe", NamedTextColor.GOLD, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Forged by a true craftsman", NamedTextColor.GRAY, TextDecoration.ITALIC));
        lore.add(Component.text("Efficiency X", NamedTextColor.BLUE));
        lore.add(Component.text("Unbreaking V", NamedTextColor.BLUE));
        lore.add(Component.text("Fortune IV", NamedTextColor.BLUE));
        meta.lore(lore);
        result.setItemMeta(meta);
        
        return new CustomRecipe("master_pickaxe", "Master's Pickaxe", "A legendary pickaxe of unmatched quality", result);
    }
    
    private CustomRecipe createMasterSword() {
        ItemStack result = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = result.getItemMeta();
        meta.displayName(Component.text("Master's Blade", NamedTextColor.RED, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Crafted by a legendary smith", NamedTextColor.GRAY, TextDecoration.ITALIC));
        lore.add(Component.text("Sharpness VII", NamedTextColor.BLUE));
        lore.add(Component.text("Unbreaking V", NamedTextColor.BLUE));
        lore.add(Component.text("Looting IV", NamedTextColor.BLUE));
        meta.lore(lore);
        result.setItemMeta(meta);
        
        return new CustomRecipe("master_sword", "Master's Blade", "A sword of legendary sharpness", result);
    }
    
    private CustomRecipe createMasterAxe() {
        ItemStack result = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = result.getItemMeta();
        meta.displayName(Component.text("Master's Axe", NamedTextColor.DARK_GREEN, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Hewn by expert hands", NamedTextColor.GRAY, TextDecoration.ITALIC));
        lore.add(Component.text("Sharpness VI", NamedTextColor.BLUE));
        lore.add(Component.text("Efficiency VIII", NamedTextColor.BLUE));
        lore.add(Component.text("Unbreaking V", NamedTextColor.BLUE));
        meta.lore(lore);
        result.setItemMeta(meta);
        
        return new CustomRecipe("master_axe", "Master's Axe", "An axe that cuts through anything", result);
    }
    
    private CustomRecipe createEfficiencyCharm() {
        ItemStack result = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = result.getItemMeta();
        meta.displayName(Component.text("Efficiency Charm", NamedTextColor.AQUA, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("A mystical charm of productivity", NamedTextColor.GRAY, TextDecoration.ITALIC));
        lore.add(Component.text("+10% XP Gain", NamedTextColor.GREEN));
        lore.add(Component.text("+5% Efficiency", NamedTextColor.GREEN));
        meta.lore(lore);
        result.setItemMeta(meta);
        
        return new CustomRecipe("efficiency_charm", "Efficiency Charm", "Boosts your productivity", result);
    }
    
    private CustomRecipe createExperienceCondenser() {
        ItemStack result = new ItemStack(Material.EXPERIENCE_BOTTLE, 16);
        ItemMeta meta = result.getItemMeta();
        meta.displayName(Component.text("Condensed Experience", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Pure crystallized knowledge", NamedTextColor.GRAY, TextDecoration.ITALIC));
        lore.add(Component.text("5x XP Value", NamedTextColor.GREEN));
        meta.lore(lore);
        result.setItemMeta(meta);
        
        return new CustomRecipe("experience_condenser", "Condensed Experience", "Concentrated experience bottles", result);
    }
    
    private CustomRecipe createFortuneAmulet() {
        ItemStack result = new ItemStack(Material.EMERALD);
        ItemMeta meta = result.getItemMeta();
        meta.displayName(Component.text("Fortune Amulet", NamedTextColor.GOLD, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Blessed by the gods of fortune", NamedTextColor.GRAY, TextDecoration.ITALIC));
        lore.add(Component.text("+50% Discovery Chance", NamedTextColor.GREEN));
        lore.add(Component.text("+25% Bonus Drops", NamedTextColor.GREEN));
        meta.lore(lore);
        result.setItemMeta(meta);
        
        return new CustomRecipe("fortune_amulet", "Fortune Amulet", "Increases your luck dramatically", result);
    }
    
    public Map<String, CustomRecipe> getCustomRecipes() {
        return new HashMap<>(customRecipes);
    }
    
    public static class CustomRecipe {
        private final String id;
        private final String name;
        private final String description;
        private final ItemStack result;
        private double requiredEfficiency;
        
        public CustomRecipe(String id, String name, String description, ItemStack result) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.result = result;
        }
        
        public void registerRecipe(Atom plugin, Player player) {
            NamespacedKey key = new NamespacedKey(plugin, id);
            
            if (Bukkit.getRecipe(key) == null) {
                ShapelessRecipe recipe = new ShapelessRecipe(key, result);
                recipe.addIngredient(Material.DIAMOND_BLOCK);
                recipe.addIngredient(Material.NETHERITE_INGOT);
                recipe.addIngredient(Material.NETHER_STAR);
                
                Bukkit.addRecipe(recipe);
            }
            
            player.discoverRecipe(key);
        }
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public ItemStack getResult() {
            return result.clone();
        }
        
        public double getRequiredEfficiency() {
            return requiredEfficiency;
        }
        
        public void setRequiredEfficiency(double requiredEfficiency) {
            this.requiredEfficiency = requiredEfficiency;
        }
    }
}
