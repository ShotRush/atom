package org.shotrush.atom.progression;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RecipeManager implements Listener {
    
    private final UnlockSystem unlockSystem;
    private final Map<UUID, Set<NamespacedKey>> playerDiscoveredRecipes;
    private final org.shotrush.atom.manager.PlayerDataManager dataManager;
    private final org.shotrush.atom.tree.Trees.Registry treeRegistry;
    
    public RecipeManager(UnlockSystem unlockSystem, org.shotrush.atom.manager.PlayerDataManager dataManager, 
                         org.shotrush.atom.tree.Trees.Registry treeRegistry) {
        this.unlockSystem = unlockSystem;
        this.playerDiscoveredRecipes = new ConcurrentHashMap<>();
        this.dataManager = dataManager;
        this.treeRegistry = treeRegistry;
    }
    

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;
        
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) {
            event.getInventory().setResult(null);System.out.println("[Recipe Block] No player data for " + player.getName());
            return;
        }
        
        Map<String, SkillNode> allNodes = buildNodeIndex();
        PlayerSkillData data = dataOpt.get();
        
        boolean canCraft = checkRecipeUnlock(player, data, allNodes, recipe);
        
        if (!canCraft) {
            String itemName = recipe.getResult().getType().name();
            System.out.println("[Recipe Block] Blocked " + itemName + " for " + player.getName() + 
                " (insufficient skill progression)");
            
            event.getInventory().setResult(null);
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                "Insufficient skill",
                net.kyori.adventure.text.format.NamedTextColor.GRAY
            ));
        } else {
            String itemName = recipe.getResult().getType().name();
            System.out.println("[Recipe Allow] " + player.getName() + " can craft " + itemName);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        Player player = event.getPlayer();
        NamespacedKey recipeKey = event.getRecipe();
        
        Recipe recipe = Bukkit.getRecipe(recipeKey);
        if (recipe == null) return;
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) {
            event.setCancelled(true);
            System.out.println("[Recipe Discovery Block] No player data for " + player.getName());
            return;
        }
        
        Map<String, SkillNode> allNodes = buildNodeIndex();
        boolean hasUnlock = checkRecipeUnlock(player, dataOpt.get(), allNodes, recipe);
        
        if (!hasUnlock) {
            String itemName = recipe.getResult().getType().name();
            System.out.println("[Recipe Discovery Block] Blocked " + itemName + " discovery for " + 
                player.getName() + " (insufficient skill)");
            
            event.setCancelled(true);
        } else {
            String itemName = recipe.getResult().getType().name();
            System.out.println("[Recipe Discovery Allow] " + player.getName() + " discovered " + itemName);
            
            playerDiscoveredRecipes.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(recipeKey);
        }
    }
    
    private Map<String, SkillNode> buildNodeIndex() {
        Map<String, SkillNode> index = new HashMap<>();
        for (var tree : treeRegistry.getAllTrees()) {
            for (String skillId : tree.getAllSkillIds()) {
                tree.getNode(skillId).ifPresent(node -> index.put(skillId, node));
            }
        }
        return index;
    }
    
    
    public void updatePlayerRecipes(Player player, PlayerSkillData playerData, Map<String, SkillNode> allNodes) {
        Set<NamespacedKey> currentRecipes = playerDiscoveredRecipes.getOrDefault(player.getUniqueId(), new HashSet<>());
        Set<NamespacedKey> shouldHaveRecipes = calculateAllowedRecipes(player, playerData, allNodes);
        
        
        Set<NamespacedKey> toRemove = new HashSet<>(currentRecipes);
        toRemove.removeAll(shouldHaveRecipes);
        
        for (NamespacedKey key : toRemove) {
            player.undiscoverRecipe(key);
            currentRecipes.remove(key);
        }
        
        
        Set<NamespacedKey> toGrant = new HashSet<>(shouldHaveRecipes);
        toGrant.removeAll(currentRecipes);
        
        for (NamespacedKey key : toGrant) {
            player.discoverRecipe(key);
            currentRecipes.add(key);
        }
        
        playerDiscoveredRecipes.put(player.getUniqueId(), currentRecipes);
    }
    
    
    private Set<NamespacedKey> calculateAllowedRecipes(Player player, PlayerSkillData playerData, Map<String, SkillNode> allNodes) {
        Set<NamespacedKey> allowed = new HashSet<>();
        
        
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            
            
            if (checkRecipeUnlock(player, playerData, allNodes, recipe)) {
                allowed.add(recipe.getResult().getType().getKey());
            }
        }
        
        return allowed;
    }
    
    
    private boolean checkRecipeUnlock(Player player, Recipe recipe) {
        
        
        return true;
    }
    
    
    private boolean checkRecipeUnlock(Player player, PlayerSkillData playerData, Map<String, SkillNode> allNodes, Recipe recipe) {
        Material result = recipe.getResult().getType();
        
        if (isBasicRecipe(result)) {
            return true;
        }
        
        String skillId = getSkillIdForRecipe(result);
        if (skillId == null) {
            System.out.println("[Recipe Debug] No skill mapping for: " + result.name());
            return true;
        }
        
        SkillNode node = allNodes.get(skillId);
        if (node == null) {
            System.out.println("[Recipe Debug] Node not found: " + skillId);
            return true;
        }
        
        long intrinsicXp = playerData.getIntrinsicXp(skillId);
        double progress = intrinsicXp / (double) node.maxXp();
        double threshold = getThresholdForMaterial(result);
        
        System.out.println("[Recipe Debug] " + result.name() + " requires " + skillId + 
            " progress: " + String.format("%.2f%%", progress * 100) + 
            " threshold: " + String.format("%.2f%%", threshold * 100));
        
        return progress >= threshold;
    }
    
    private boolean isBasicRecipe(Material material) {
        String name = material.name().toLowerCase();
        return name.contains("planks") || name.equals("stick") || name.equals("crafting_table") ||
               name.startsWith("wooden_") || name.equals("torch") || name.equals("chest");
    }
    
    private String getSkillIdForRecipe(Material material) {
        String name = material.name().toLowerCase();
        
        if (name.contains("stone") && !name.contains("bricks")) {
            return "miner";
        }
        
        if (name.contains("iron") || name.contains("gold") || name.contains("diamond") || 
            name.contains("netherite")) {
            return "blacksmith";
        }
        
        if (name.contains("furnace") || name.contains("anvil") || name.contains("smithing")) {
            return "blacksmith";
        }
        
        if (name.contains("enchant") || name.contains("book")) {
            return "librarian";
        }
        
        if (name.contains("brewing") || name.contains("potion") || name.contains("cauldron")) {
            return "healer";
        }
        
        if (name.contains("hoe") || name.contains("composter")) {
            return "farmer";
        }
        
        if (name.contains("bricks") || name.contains("stairs") || name.contains("slab") || 
            name.contains("wall") || name.contains("fence")) {
            return "builder";
        }
        
        return null;
    }
    
    private double getThresholdForMaterial(Material material) {
        String name = material.name().toLowerCase();
        
        if (name.contains("stone")) return 0.05;
        if (name.contains("iron")) return 0.15;
        if (name.contains("gold")) return 0.20;
        if (name.contains("diamond")) return 0.40;
        if (name.contains("netherite")) return 0.70;
        
        return 0.10;
    }
    
    
    public void initializePlayerRecipes(Player player, PlayerSkillData playerData) {
        Map<String, SkillNode> allNodes = buildNodeIndex();
        
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            
            if (isBasicRecipe(recipe.getResult().getType())) {
                player.discoverRecipe(recipe.getResult().getType().getKey());
                System.out.println("[Recipe Init] Granted basic recipe " + recipe.getResult().getType().name() + 
                    " to " + player.getName());
            }
        }
        
        updatePlayerRecipes(player, playerData, allNodes);
    }
    
    
    public void clearPlayerRecipes(UUID playerId) {
        playerDiscoveredRecipes.remove(playerId);
    }
    
    
    public Set<NamespacedKey> getDiscoveredRecipes(UUID playerId) {
        return new HashSet<>(playerDiscoveredRecipes.getOrDefault(playerId, new HashSet<>()));
    }
}
