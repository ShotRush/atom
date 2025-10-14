package org.shotrush.atom.manager;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.PlayerData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicActionManager {
    private final Atom plugin;
    private final PlayerDataManager playerDataManager;
    private final Map<String, DynamicAction> dynamicActions;
    private final Map<ActionCategory, Set<String>> categoryGroups;
    
    public DynamicActionManager(Atom plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.dynamicActions = new ConcurrentHashMap<>();
        this.categoryGroups = new HashMap<>();
        
        for (ActionCategory category : ActionCategory.values()) {
            categoryGroups.put(category, ConcurrentHashMap.newKeySet());
        }
    }
    
    public void trackBlockBreak(Player player, BlockBreakEvent event) {
        Material material = event.getBlock().getType();
        String actionId = generateActionId("break", material);
        
        DynamicAction action = getOrCreateAction(actionId, "Breaking " + formatName(material.name()), ActionCategory.MINING);
        grantExperience(player, actionId, calculateExperience(action));
    }
    
    public void trackBlockPlace(Player player, BlockPlaceEvent event) {
        Material material = event.getBlock().getType();
        String actionId = generateActionId("place", material);
        
        DynamicAction action = getOrCreateAction(actionId, "Placing " + formatName(material.name()), ActionCategory.BUILDING);
        grantExperience(player, actionId, calculateExperience(action));
    }
    
    public void trackEntityKill(Player player, EntityDeathEvent event) {
        EntityType entityType = event.getEntity().getType();
        String actionId = generateActionId("kill", entityType);
        
        DynamicAction action = getOrCreateAction(actionId, "Hunting " + formatName(entityType.name()), ActionCategory.COMBAT);
        grantExperience(player, actionId, calculateExperience(action));
    }
    
    public void trackCrafting(Player player, CraftItemEvent event) {
        Material result = event.getRecipe().getResult().getType();
        String actionId = generateActionId("craft", result);
        
        DynamicAction action = getOrCreateAction(actionId, "Crafting " + formatName(result.name()), ActionCategory.CRAFTING);
        grantExperience(player, actionId, calculateExperience(action));
    }
    
    public void trackFishing(Player player, PlayerFishEvent event) {
        String actionId = "fish_general";
        
        DynamicAction action = getOrCreateAction(actionId, "Fishing", ActionCategory.GATHERING);
        grantExperience(player, actionId, calculateExperience(action));
    }
    
    private String generateActionId(String prefix, Material material) {
        return prefix + "_" + material.name().toLowerCase();
    }
    
    private String generateActionId(String prefix, EntityType entityType) {
        return prefix + "_" + entityType.name().toLowerCase();
    }
    
    private DynamicAction getOrCreateAction(String actionId, String displayName, ActionCategory category) {
        return dynamicActions.computeIfAbsent(actionId, id -> {
            DynamicAction action = new DynamicAction(id, displayName, category);
            categoryGroups.get(category).add(actionId);
            plugin.getLogger().info("Created dynamic action: " + displayName + " (" + actionId + ")");
            return action;
        });
    }
    
    private void grantExperience(Player player, String actionId, double experience) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        double currentXP = data.getExperience(actionId);
        data.setExperience(actionId, currentXP + experience);
        
        applyDynamicSkillTransfer(player, actionId, experience);
        
        DynamicAction action = dynamicActions.get(actionId);
        if (action != null) {
            action.incrementCount();
        }
    }
    
    private void applyDynamicSkillTransfer(Player player, String actionId, double experience) {
        if (!plugin.getSkillTransferManager().isEnabled()) return;
        
        DynamicAction action = dynamicActions.get(actionId);
        if (action == null) return;
        
        ActionCategory category = action.getCategory();
        Set<String> relatedActions = categoryGroups.get(category);
        if (relatedActions == null) return;
        
        double transferRate = plugin.getSkillTransferManager().getTransferRate();
        double transferAmount = experience * transferRate;
        
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        
        for (String relatedActionId : relatedActions) {
            if (!relatedActionId.equals(actionId)) {
                data.addExperience(relatedActionId, transferAmount);
            }
        }
    }
    
    private double calculateExperience(DynamicAction action) {
        switch (action.getCategory()) {
            case MINING:
                return 1.0;
            case BUILDING:
                return 0.5;
            case COMBAT:
                return 2.0;
            case CRAFTING:
                return 1.5;
            case GATHERING:
                return 1.0;
            default:
                return 1.0;
        }
    }
    
    private String formatName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (formatted.length() > 0) {
                formatted.append(" ");
            }
            formatted.append(Character.toUpperCase(word.charAt(0)))
                     .append(word.substring(1));
        }
        
        return formatted.toString();
    }
    
    public Map<String, DynamicAction> getDynamicActions() {
        return new HashMap<>(dynamicActions);
    }
    
    public DynamicAction getAction(String actionId) {
        return dynamicActions.get(actionId);
    }
    
    public List<DynamicAction> getActionsByCategory(ActionCategory category) {
        List<DynamicAction> result = new ArrayList<>();
        for (DynamicAction action : dynamicActions.values()) {
            if (action.getCategory() == category) {
                result.add(action);
            }
        }
        return result;
    }
    
    public List<DynamicAction> getPlayerActions(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        Map<String, Double> experience = data.getActionExperience();
        
        List<DynamicAction> result = new ArrayList<>();
        for (String actionId : experience.keySet()) {
            DynamicAction action = dynamicActions.get(actionId);
            if (action != null) {
                result.add(action);
            }
        }
        
        return result;
    }
    
    public int getTotalUniqueActions() {
        return dynamicActions.size();
    }
    
    public void pruneInactiveActions(long inactiveDays) {
        long cutoffTime = System.currentTimeMillis() - (inactiveDays * 24 * 60 * 60 * 1000);
        
        dynamicActions.entrySet().removeIf(entry -> {
            DynamicAction action = entry.getValue();
            return action.getTotalCount() == 0 && action.getCreatedAt() < cutoffTime;
        });
    }
    
    public static class DynamicAction {
        private final String id;
        private final String displayName;
        private final ActionCategory category;
        private final long createdAt;
        private long totalCount;
        
        public DynamicAction(String id, String displayName, ActionCategory category) {
            this.id = id;
            this.displayName = displayName;
            this.category = category;
            this.createdAt = System.currentTimeMillis();
            this.totalCount = 0;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public ActionCategory getCategory() {
            return category;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
        
        public long getTotalCount() {
            return totalCount;
        }
        
        public void incrementCount() {
            totalCount++;
        }
    }
    
    public enum ActionCategory {
        MINING("Mining & Excavation"),
        BUILDING("Construction & Building"),
        COMBAT("Combat & Hunting"),
        CRAFTING("Crafting & Creation"),
        GATHERING("Gathering & Harvesting"),
        EXPLORATION("Exploration & Discovery"),
        TRADING("Trading & Commerce"),
        FARMING("Farming & Agriculture");
        
        private final String displayName;
        
        ActionCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
