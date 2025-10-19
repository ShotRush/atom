package org.shotrush.atom.progression;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.inventory.Recipe;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RecipeManager implements Listener {
    
    private final UnlockSystem unlockSystem;
    private final Map<UUID, Set<NamespacedKey>> playerDiscoveredRecipes;
    
    public RecipeManager(UnlockSystem unlockSystem) {
        this.unlockSystem = unlockSystem;
        this.playerDiscoveredRecipes = new ConcurrentHashMap<>();
    }
    

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        Player player = event.getPlayer();
        NamespacedKey recipeKey = event.getRecipe();
        
        
        
        Recipe recipe = Bukkit.getRecipe(recipeKey);
        if (recipe == null) return;
        
        
        
        boolean hasUnlock = checkRecipeUnlock(player, recipe);
        
        if (!hasUnlock) {
            
            event.setCancelled(true);
            
            
            player.sendMessage("§cYou need to progress further in your skills to unlock this recipe!");
        } else {
            
            playerDiscoveredRecipes.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(recipeKey);
        }
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
        
        return unlockSystem.canCraft(player, recipe.getResult().getType(), playerData, allNodes);
    }
    
    
    public void grantBasicRecipes(Player player) {
        
        List<String> basicRecipes = Arrays.asList(
            "crafting_table",
            "stick",
            "wooden_pickaxe",
            "wooden_axe",
            "wooden_shovel",
            "wooden_hoe",
            "wooden_sword",
            "torch"
        );
        
        for (String recipeName : basicRecipes) {
            NamespacedKey key = NamespacedKey.minecraft(recipeName);
            if (Bukkit.getRecipe(key) != null) {
                player.discoverRecipe(key);
            }
        }
    }
    
    
    public void clearPlayerRecipes(UUID playerId) {
        playerDiscoveredRecipes.remove(playerId);
    }
    
    
    public Set<NamespacedKey> getDiscoveredRecipes(UUID playerId) {
        return new HashSet<>(playerDiscoveredRecipes.getOrDefault(playerId, new HashSet<>()));
    }
}
