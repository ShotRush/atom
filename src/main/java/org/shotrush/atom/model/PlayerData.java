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
    
    private boolean isBigMan;
    private double bigManScore;
    private int bigManFollowers;
    private int bigManRedistributions;
    private String authorityType;
    private double legitimacy;
    
    private final Map<UUID, Integer> tradeNetwork;
    private double socialCapital;
    private final Map<String, Integer> publicProjectContributions;
    private double foodSurplus;

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.actionExperience = new HashMap<>();
        this.completedMilestones = new HashSet<>();
        this.discoveredRecipes = new HashSet<>();
        this.modified = false;
        this.dataVersion = DATA_VERSION;
        this.isBigMan = false;
        this.bigManScore = 0.0;
        this.bigManFollowers = 0;
        this.bigManRedistributions = 0;
        this.authorityType = "NONE";
        this.legitimacy = 0.0;
        this.tradeNetwork = new HashMap<>();
        this.socialCapital = 0.0;
        this.publicProjectContributions = new HashMap<>();
        this.foodSurplus = 0.0;
    }

    public PlayerData(UUID playerId, Map<String, Double> actionExperience, Set<String> completedMilestones) {
        this.playerId = playerId;
        this.actionExperience = new HashMap<>(actionExperience);
        this.completedMilestones = new HashSet<>(completedMilestones);
        this.discoveredRecipes = new HashSet<>();
        this.modified = false;
        this.isBigMan = false;
        this.bigManScore = 0.0;
        this.bigManFollowers = 0;
        this.bigManRedistributions = 0;
        this.authorityType = "NONE";
        this.legitimacy = 0.0;
        this.tradeNetwork = new HashMap<>();
        this.socialCapital = 0.0;
        this.publicProjectContributions = new HashMap<>();
        this.foodSurplus = 0.0;
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

    public boolean isBigMan() {
        return isBigMan;
    }

    public void setBigMan(boolean bigMan) {
        this.isBigMan = bigMan;
        this.modified = true;
    }

    public double getBigManScore() {
        return bigManScore;
    }

    public void setBigManScore(double score) {
        this.bigManScore = score;
        this.modified = true;
    }

    public int getBigManFollowers() {
        return bigManFollowers;
    }

    public void setBigManFollowers(int followers) {
        this.bigManFollowers = followers;
        this.modified = true;
    }

    public int getBigManRedistributions() {
        return bigManRedistributions;
    }

    public void setBigManRedistributions(int redistributions) {
        this.bigManRedistributions = redistributions;
        this.modified = true;
    }

    public void incrementRedistributions() {
        this.bigManRedistributions++;
        this.modified = true;
    }

    public String getAuthorityType() {
        return authorityType;
    }

    public void setAuthorityType(String type) {
        this.authorityType = type;
        this.modified = true;
    }

    public double getLegitimacy() {
        return legitimacy;
    }

    public void setLegitimacy(double legitimacy) {
        this.legitimacy = legitimacy;
        this.modified = true;
    }

    public Map<UUID, Integer> getTradeNetwork() {
        return new HashMap<>(tradeNetwork);
    }

    public void recordTrade(UUID partner) {
        tradeNetwork.put(partner, tradeNetwork.getOrDefault(partner, 0) + 1);
        this.modified = true;
    }

    public void setTradeNetwork(Map<UUID, Integer> network) {
        this.tradeNetwork.clear();
        this.tradeNetwork.putAll(network);
        this.modified = true;
    }

    public int getTradeCount(UUID partner) {
        return tradeNetwork.getOrDefault(partner, 0);
    }

    public double getSocialCapital() {
        return socialCapital;
    }

    public void setSocialCapital(double capital) {
        this.socialCapital = capital;
        this.modified = true;
    }

    public Map<String, Integer> getPublicProjectContributions() {
        return new HashMap<>(publicProjectContributions);
    }

    public void contributeToProject(String projectName, int amount) {
        publicProjectContributions.put(projectName, 
            publicProjectContributions.getOrDefault(projectName, 0) + amount);
        this.modified = true;
    }

    public void setPublicProjectContributions(Map<String, Integer> contributions) {
        this.publicProjectContributions.clear();
        this.publicProjectContributions.putAll(contributions);
        this.modified = true;
    }

    public double getFoodSurplus() {
        return foodSurplus;
    }

    public void setFoodSurplus(double surplus) {
        this.foodSurplus = surplus;
        this.modified = true;
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


