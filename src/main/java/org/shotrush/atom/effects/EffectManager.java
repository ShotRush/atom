package org.shotrush.atom.effects;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.config.AtomConfig;
import org.shotrush.atom.manager.PlayerDataManager;
import org.shotrush.atom.engine.XpEngine;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;
import org.shotrush.atom.progression.AttributeModifierSystem;
import org.shotrush.atom.progression.DepthProgression;
import org.shotrush.atom.progression.DepthProgression.SpecializationMetrics;
import org.shotrush.atom.progression.RecipeManager;
import org.shotrush.atom.progression.UnlockSystem;
import org.shotrush.atom.tree.SkillTreeRegistry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class EffectManager {
    
    private final Plugin plugin;
    private final AtomConfig config;
    private final XpEngine xpEngine;
    private final SkillTreeRegistry treeRegistry;
    private final AttributeModifierSystem attributeSystem;
    private final UnlockSystem unlockSystem;
    private final RecipeManager recipeManager;
    private final Map<UUID, SpecializationCache> specializationCache;
    
    public EffectManager(Plugin plugin, AtomConfig config, XpEngine xpEngine, SkillTreeRegistry treeRegistry, 
                         PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.config = config;
        this.xpEngine = xpEngine;
        this.treeRegistry = treeRegistry;
        this.attributeSystem = new AttributeModifierSystem(plugin);
        this.unlockSystem = new UnlockSystem();
        this.recipeManager = new RecipeManager(unlockSystem, dataManager, treeRegistry);
        this.specializationCache = new ConcurrentHashMap<>();
    }
    
    
    public void updatePlayerEffects(Player player, PlayerSkillData data) {
        
        Map<String, SkillNode> allNodes = buildNodeIndex();
        
        
        Map<String, SpecializationMetrics> metrics = DepthProgression.calculateSpecialization(data, allNodes);
        
        
        specializationCache.put(player.getUniqueId(), new SpecializationCache(metrics, allNodes));
        
        
        if (config.enableBonuses() || config.enablePenalties()) {
            attributeSystem.updatePlayerAttributes(player, data, allNodes, metrics);
        }
        
        
        recipeManager.updatePlayerRecipes(player, data, allNodes);
    }
    
    
    private Map<String, SkillNode> buildNodeIndex() {
        Map<String, SkillNode> allNodes = new ConcurrentHashMap<>();
        
        treeRegistry.getAllTrees().forEach(tree -> {
            tree.getAllSkillIds().forEach(skillId -> {
                tree.getNode(skillId).ifPresent(node -> allNodes.put(skillId, node));
            });
        });
        
        return allNodes;
    }
    
    
    public double getFarmingDropRateMultiplier(Player player, PlayerSkillData data) {
        SpecializationCache cache = specializationCache.get(player.getUniqueId());
        if (cache == null) return 1.0;
        
        SpecializationMetrics farmerMetrics = cache.metrics.get("farmer");
        if (farmerMetrics == null) return 0.5; 
        
        
        return 1.0 + (farmerMetrics.specializationScore() * 0.5); 
    }
    
    
    public double getToolDurabilityMultiplier(Player player, String category, PlayerSkillData data) {
        SpecializationCache cache = specializationCache.get(player.getUniqueId());
        if (cache == null) return 1.5; 
        
        SpecializationMetrics metrics = cache.metrics.get(category);
        if (metrics == null) return 1.5; 
        
        
        return 1.0 - (metrics.specializationScore() * 0.3); 
    }
    
    
    public void clearPlayerEffects(UUID playerId) {
        specializationCache.remove(playerId);
    }
    
    
    public UnlockSystem getUnlockSystem() {
        return unlockSystem;
    }
    
    
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
    
    
    public Map<String, SpecializationMetrics> getSpecializationMetrics(UUID playerId) {
        SpecializationCache cache = specializationCache.get(playerId);
        return cache != null ? cache.metrics : Map.of();
    }
    
    
    private record SpecializationCache(
        Map<String, SpecializationMetrics> metrics,
        Map<String, SkillNode> allNodes
    ) {}
}
