package org.shotrush.atom.manager;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.persistence.PersistentDataType;
import org.shotrush.atom.Atom;

import java.util.*;

public class CraftingProjectManager {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final EmergentBonusManager emergentBonusManager;
    
    private final NamespacedKey projectKey;
    private final NamespacedKey recipeKey;
    private final NamespacedKey componentsKey;
    
    private boolean enabled;
    private double baseSuccessChance;
    private double efficiencySuccessBonus;
    private double maxSuccessChance;
    private int consumptionTime;
    private String fallbackActionId;
    private Set<Material> convertibleMaterials;
    
    private int foodBaseNutrition;
    private double foodNutritionPerComponent;
    private float foodSaturation;
    private boolean foodCanAlwaysEat;
    
    public CraftingProjectManager(Atom plugin, ConfigManager configManager, EmergentBonusManager emergentBonusManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.emergentBonusManager = emergentBonusManager;
        
        this.projectKey = new NamespacedKey(plugin, "crafting_project");
        this.recipeKey = new NamespacedKey(plugin, "project_recipe");
        this.componentsKey = new NamespacedKey(plugin, "project_components");
        
        setDefaults();
        loadConfig();
    }
    
    private void setDefaults() {
        this.enabled = true;
        this.baseSuccessChance = 0.5;
        this.efficiencySuccessBonus = 0.1;
        this.maxSuccessChance = 0.95;
        this.consumptionTime = 32;
        this.fallbackActionId = "craft_tools";
        this.convertibleMaterials = new HashSet<>();
        
        this.foodBaseNutrition = 2;
        this.foodNutritionPerComponent = 0.5;
        this.foodSaturation = 0.5f;
        this.foodCanAlwaysEat = true;
    }
    
    private void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("crafting-projects.enabled", enabled);
        this.baseSuccessChance = plugin.getConfig().getDouble("crafting-projects.base-success-chance", baseSuccessChance);
        this.efficiencySuccessBonus = plugin.getConfig().getDouble("crafting-projects.efficiency-success-bonus", efficiencySuccessBonus);
        this.maxSuccessChance = plugin.getConfig().getDouble("crafting-projects.max-success-chance", maxSuccessChance);
        this.consumptionTime = plugin.getConfig().getInt("crafting-projects.consumption-time-ticks", consumptionTime);
        this.fallbackActionId = plugin.getConfig().getString("crafting-projects.fallback-action-id", fallbackActionId);
        
        this.foodBaseNutrition = plugin.getConfig().getInt("crafting-projects.food.base-nutrition", foodBaseNutrition);
        this.foodNutritionPerComponent = plugin.getConfig().getDouble("crafting-projects.food.nutrition-per-component", foodNutritionPerComponent);
        this.foodSaturation = (float) plugin.getConfig().getDouble("crafting-projects.food.saturation", foodSaturation);
        this.foodCanAlwaysEat = plugin.getConfig().getBoolean("crafting-projects.food.can-always-eat", foodCanAlwaysEat);
        
        List<String> materialNames = plugin.getConfig().getStringList("crafting-projects.convertible-materials");
        convertibleMaterials.clear();
        for (String materialName : materialNames) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                convertibleMaterials.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in crafting-projects.convertible-materials: " + materialName);
            }
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getConsumptionTime() {
        return consumptionTime;
    }
    
    public Set<Material> getConvertibleMaterials() {
        return new HashSet<>(convertibleMaterials);
    }
    
    public String getFallbackActionId() {
        return fallbackActionId;
    }
    
    /**
     * Creates a crafting project (blueprint map) for a given recipe
     */
    public ItemStack createCraftingProject(Material resultMaterial, List<ItemStack> components) {
        ItemStack map = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = map.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text("Crafting Project: ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(formatMaterialName(resultMaterial), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Right-click and hold to attempt crafting", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(""));
            lore.add(Component.text("Components Required:", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
            
            for (ItemStack component : components) {
                if (component != null && component.getType() != Material.AIR) {
                    lore.add(Component.text("  • ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(component.getAmount() + "x ", NamedTextColor.WHITE))
                        .append(Component.text(formatMaterialName(component.getType()), NamedTextColor.GRAY))
                        .decoration(TextDecoration.ITALIC, false));
                }
            }
            
            lore.add(Component.text(""));
            lore.add(Component.text("⚠ Success is not guaranteed!", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
            
            
            meta.getPersistentDataContainer().set(projectKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(recipeKey, PersistentDataType.STRING, resultMaterial.name());
            meta.getPersistentDataContainer().set(componentsKey, PersistentDataType.INTEGER, components.size());
            
            map.setItemMeta(meta);
        }

        int totalNutrition = foodBaseNutrition + (int) (components.size() * foodNutritionPerComponent);
        
        FoodProperties food = FoodProperties.food()
            .nutrition(totalNutrition)
            .saturation(foodSaturation)
            .canAlwaysEat(foodCanAlwaysEat)
            .build();
        map.setData(DataComponentTypes.FOOD, food);
        
        Consumable consumable = Consumable.consumable()
            .consumeSeconds(consumptionTime / 20.0f)
            .animation(ItemUseAnimation.EAT)
            .sound(Key.key("entity.generic.eat"))
            .hasConsumeParticles(true)
            .build();
        map.setData(DataComponentTypes.CONSUMABLE, consumable);
        
        return map;
    }

    public boolean isCraftingProject(ItemStack item) {
        if (item == null || item.getType() != Material.FILLED_MAP) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        return meta.getPersistentDataContainer().has(projectKey, PersistentDataType.BYTE);
    }
    
    /**
     * Gets the result material from a crafting project
     */
    public Material getProjectResult(ItemStack project) {
        if (!isCraftingProject(project)) return null;
        
        ItemMeta meta = project.getItemMeta();
        if (meta == null) return null;
        
        String materialName = meta.getPersistentDataContainer().get(recipeKey, PersistentDataType.STRING);
        if (materialName == null) return null;
        
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Calculates success chance based on player's crafting efficiency
     */
    public double calculateSuccessChance(Player player, Material resultMaterial) {
        String actionId = findCraftingAction(resultMaterial);
        if (actionId == null) {
            actionId = fallbackActionId;
        }
        
        double efficiency = emergentBonusManager.getSpeedMultiplier(player, actionId);
        double successChance = baseSuccessChance + (efficiency * efficiencySuccessBonus);
        
        return Math.min(successChance, maxSuccessChance);
    }
    

    public CraftingResult attemptCrafting(Player player, ItemStack project) {
        Material resultMaterial = getProjectResult(project);
        if (resultMaterial == null) {
            return new CraftingResult(false, null, "Invalid crafting project!");
        }
        
        double successChance = calculateSuccessChance(player, resultMaterial);
        boolean success = Math.random() < successChance;
        
        if (success) {
            ItemStack result = new ItemStack(resultMaterial, 1);
            
            if (result.getType().getMaxDurability() > 0) {
                result.setData(DataComponentTypes.DAMAGE, 0);
            }
            
            return new CraftingResult(true, result, null);
        } else {
            return new CraftingResult(false, null, "You have botched the crafting project!");
        }
    }
    
    /**
     * Finds the crafting action ID for a material
     */
    public String findCraftingAction(Material material) {
        for (Map.Entry<String, org.shotrush.atom.model.TrackedAction> entry : configManager.getActions().entrySet()) {
            org.shotrush.atom.model.TrackedAction action = entry.getValue();
            for (org.shotrush.atom.model.TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == org.shotrush.atom.model.TriggerType.CRAFT_ITEM) {
                    if (trigger.getMaterials().contains(material)) {
                        return entry.getKey();
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Formats material name to be more readable
     */
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
            }
        }
        
        return formatted.toString().trim();
    }
    
    /**
     * Result of a crafting attempt
     */
    public static class CraftingResult {
        private final boolean success;
        private final ItemStack result;
        private final String failureMessage;
        
        public CraftingResult(boolean success, ItemStack result, String failureMessage) {
            this.success = success;
            this.result = result;
            this.failureMessage = failureMessage;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public ItemStack getResult() {
            return result;
        }
        
        public String getFailureMessage() {
            return failureMessage;
        }
    }
}
