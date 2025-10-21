package org.shotrush.atom.effects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
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

import java.time.Duration;
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
    
    
    public void sendXpGain(Player player, String skillId, long amount) {
        if (!config.enableFeedback()) return;
        
        Component message = Component.text("+ ", NamedTextColor.GREEN)
            .append(Component.text(amount, NamedTextColor.GOLD))
            .append(Component.text(" XP ", NamedTextColor.GREEN))
            .append(Component.text("(" + formatSkillId(skillId) + ")", NamedTextColor.GRAY));
        
        player.sendActionBar(message);
    }
    
    public void sendPenalty(Player player, String skillId, double penaltyPercent) {
        if (!config.enableFeedback()) return;
        
        Component message = Component.text("⚠ ", NamedTextColor.RED)
            .append(Component.text(String.format("%.0f%%", penaltyPercent * 100), NamedTextColor.YELLOW))
            .append(Component.text(" penalty ", NamedTextColor.RED))
            .append(Component.text("(" + formatSkillId(skillId) + ")", NamedTextColor.GRAY));
        
        player.sendActionBar(message);
    }
    
    public void sendBonus(Player player, String skillId, double bonusPercent) {
        if (!config.enableFeedback()) return;
        
        Component message = Component.text("✓ ", NamedTextColor.GREEN)
            .append(Component.text(String.format("%.0f%%", bonusPercent * 100), NamedTextColor.GOLD))
            .append(Component.text(" bonus ", NamedTextColor.GREEN))
            .append(Component.text("(" + formatSkillId(skillId) + ")", NamedTextColor.GRAY));
        
        player.sendActionBar(message);
    }
    
    public void sendMilestoneReached(Player player, String milestoneName) {
        if (!config.enableFeedback()) return;
        
        Component title = Component.text("Milestone Reached!", NamedTextColor.GOLD);
        Component subtitle = Component.text(milestoneName, NamedTextColor.YELLOW);
        
        player.showTitle(Title.title(
            title,
            subtitle,
            Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofSeconds(3),
                Duration.ofMillis(1000)
            )
        ));
        
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }
    
    private String formatSkillId(String skillId) {
        String[] parts = skillId.split("\\.");
        String last = parts[parts.length - 1];
        return last.substring(0, 1).toUpperCase() + last.substring(1).replace("_", " ");
    }
    
    private record SpecializationCache(
        Map<String, SpecializationMetrics> metrics,
        Map<String, SkillNode> allNodes
    ) {}
}
