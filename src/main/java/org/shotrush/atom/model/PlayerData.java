package org.shotrush.atom.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerData {
    private static final int DATA_VERSION = 2;
    
    private final UUID playerId;
    private final Map<String, Double> actionExperience;
    private final Set<String> completedMilestones;
    private final Set<String> discoveredRecipes;
    private boolean modified;
    private int dataVersion;

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.actionExperience = new HashMap<>();
        this.completedMilestones = new HashSet<>();
        this.discoveredRecipes = new HashSet<>();
        this.modified = false;
        this.dataVersion = DATA_VERSION;
    }

    public PlayerData(UUID playerId, Map<String, Double> actionExperience, Set<String> completedMilestones) {
        this.playerId = playerId;
        this.actionExperience = new HashMap<>(actionExperience);
        this.completedMilestones = new HashSet<>(completedMilestones);
        this.discoveredRecipes = new HashSet<>();
        this.modified = false;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Map<String, Double> getActionExperience() {
        return new HashMap<>(actionExperience);
    }

    public double getExperience(String actionId) {
        return actionExperience.getOrDefault(actionId, 0.0);
    }

    public void addExperience(String actionId, double amount) {
        actionExperience.put(actionId, getExperience(actionId) + amount);
        modified = true;
    }

    public void setExperience(String actionId, double amount) {
        actionExperience.put(actionId, amount);
        modified = true;
    }

    public boolean hasMilestone(String milestoneId) {
        return completedMilestones.contains(milestoneId);
    }

    public void completeMilestone(String milestoneId) {
        completedMilestones.add(milestoneId);
        modified = true;
    }

    public Set<String> getCompletedMilestones() {
        return new HashSet<>(completedMilestones);
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean hasDiscoveredRecipe(String recipeId) {
        return discoveredRecipes.contains(recipeId);
    }

    public void discoverRecipe(String recipeId) {
        discoveredRecipes.add(recipeId);
        modified = true;
    }

    public Set<String> getDiscoveredRecipes() {
        return new HashSet<>(discoveredRecipes);
    }
    
    public int getDataVersion() {
        return dataVersion;
    }
    
    public void migrateData() {
        if (dataVersion < DATA_VERSION) {
            if (dataVersion < 2 && discoveredRecipes.isEmpty()) {
            }
            dataVersion = DATA_VERSION;
            modified = true;
        }
    }
}


