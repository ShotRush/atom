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
    private final org.shotrush.atom.tree.SkillTreeRegistry treeRegistry;
    
    public RecipeManager(UnlockSystem unlockSystem, org.shotrush.atom.manager.PlayerDataManager dataManager, 
                         org.shotrush.atom.tree.SkillTreeRegistry treeRegistry) {
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
                "§cInsufficient skill to craft this item",
                net.kyori.adventure.text.format.NamedTextColor.RED
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
        
        double totalSkillScore = 0.0;
        int relevantSkills = 0;
        int totalSkillsWithXp = 0;
        
        for (Map.Entry<String, Long> entry : playerData.getAllIntrinsicXp().entrySet()) {
            SkillNode node = allNodes.get(entry.getKey());
            if (node == null) continue;
            
            if (entry.getValue() > 0) totalSkillsWithXp++;
            
            double ratio = entry.getValue() / (double) node.maxXp();
            
            if (ratio > 0.5 && node.depth() >= 2) {
                totalSkillScore += ratio;
                relevantSkills++;
                
                if (ratio > 1.5) {
                    totalSkillScore += (ratio - 1.5) * 0.5;
                }
            }
        }
        
        if (totalSkillsWithXp == 0) {
            return false;
        }
        
        if (relevantSkills >= 2 && totalSkillScore > 1.5) {
            return true;
        }
        
        return unlockSystem.canCraft(player, result, playerData, allNodes);
    }
    
    private boolean isBasicRecipe(Material material) {
        String name = material.name().toLowerCase();
        
        if (name.contains("planks") || name.equals("stick") || name.equals("crafting_table")) {
            return true;
        }
        
        if (name.startsWith("wooden_") && (name.contains("pickaxe") || name.contains("axe") || 
            name.contains("shovel") || name.contains("hoe") || name.contains("sword"))) {
            return true;
        }

        return name.equals("torch") || name.equals("chest") || name.equals("furnace");
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
