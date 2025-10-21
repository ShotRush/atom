package org.shotrush.atom.ml;

import org.bukkit.plugin.Plugin;
import org.shotrush.atom.config.TreeBuilder;
import org.shotrush.atom.config.TreeDefinition;
import org.shotrush.atom.manager.PlayerDataManager;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.storage.TreeStorage;
import org.shotrush.atom.tree.SkillTree;
import org.shotrush.atom.tree.SkillTreeRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public final class DynamicTreeManager {
    
    private final Plugin plugin;
    private final SkillTreeRegistry treeRegistry;
    private final PlayerDataManager dataManager;
    private final TreeStorage treeStorage;
    private final SkillTreeGenerator treeGenerator;
    private final Map<String, TreeDefinition> dynamicTreeCache;
    private final Map<String, Map<String, SkillTreeGenerator.ActionFrequency>> actionDataCache;
    private final Map<String, Long> lastGenerationTime;
    
    public DynamicTreeManager(
        Plugin plugin,
        SkillTreeRegistry treeRegistry,
        PlayerDataManager dataManager,
        TreeStorage treeStorage
    ) {
        this.plugin = plugin;
        this.treeRegistry = treeRegistry;
        this.dataManager = dataManager;
        this.treeStorage = treeStorage;
        this.treeGenerator = new SkillTreeGenerator(3, 0.7);
        this.dynamicTreeCache = new ConcurrentHashMap<>();
        this.actionDataCache = new ConcurrentHashMap<>();
        this.lastGenerationTime = new ConcurrentHashMap<>();
        
        loadSavedBranches();
    }
    
    
    private void loadSavedBranches() {
        List<String> rootClusters = List.of(
            "farmer", "guardsman", "miner", "healer", "blacksmith", "builder", "librarian"
        );
        
        for (String clusterId : rootClusters) {
            treeStorage.loadDynamicBranches(clusterId).thenAccept(branchesOpt -> {
                if (branchesOpt.isPresent()) {
                    List<TreeDefinition.NodeDefinition> branches = branchesOpt.get();
                    updateTreeWithBranches(clusterId, branches);
                    plugin.getLogger().info("Loaded " + branches.size() + " saved branches for " + clusterId);
                }
            });
        }
    }
    
    public void generateBranchesForCluster(String rootClusterId) {
        Map<String, SkillTreeGenerator.ActionFrequency> actionData = collectActionData(rootClusterId);
        
        if (actionData.isEmpty()) {
            plugin.getLogger().info("No action data available for cluster: " + rootClusterId);
            return;
        }
        
        List<TreeDefinition.NodeDefinition> branches = treeGenerator.generateBranches(
            rootClusterId,
            actionData,
            3
        );
        
        if (branches.isEmpty()) {
            plugin.getLogger().info("No branches generated for cluster: " + rootClusterId);
            return;
        }
        
        plugin.getLogger().info("Generated " + branches.size() + " dynamic branches for " + rootClusterId);
        
        updateTreeWithBranches(rootClusterId, branches);
        
        treeStorage.saveDynamicBranches(rootClusterId, branches);
        lastGenerationTime.put(rootClusterId, System.currentTimeMillis());
        
        saveTreeMetadata(rootClusterId);
    }
    
    
    private Map<String, SkillTreeGenerator.ActionFrequency> collectActionData(String rootClusterId) {
        Map<String, SkillTreeGenerator.ActionFrequency> aggregatedData = new HashMap<>();
        
        for (PlayerSkillData playerData : dataManager.getAllCachedData()) {
            Map<String, Long> skills = playerData.getAllIntrinsicXp();
            
            for (Map.Entry<String, Long> entry : skills.entrySet()) {
                String skillId = entry.getKey();
                Long xp = entry.getValue();
                
                if (skillId.startsWith(rootClusterId + ".")) {
                    String actionId = extractActionId(skillId);
                    String materialType = extractMaterialType(skillId);
                    
                    SkillTreeGenerator.ActionFrequency existing = aggregatedData.get(actionId);
                    if (existing != null) {
                        aggregatedData.put(actionId, new SkillTreeGenerator.ActionFrequency(
                            actionId,
                            materialType,
                            existing.totalOccurrences() + 1,
                            (existing.averageXpPerAction() + xp) / 2.0,
                            existing.metadata()
                        ));
                    } else {
                        aggregatedData.put(actionId, new SkillTreeGenerator.ActionFrequency(
                            actionId,
                            materialType,
                            1,
                            xp.doubleValue(),
                            new HashMap<>()
                        ));
                    }
                }
            }
        }
        
        return aggregatedData;
    }
    
    private String extractActionId(String skillId) {
        String[] parts = skillId.split("\\.");
        return parts[parts.length - 1];
    }
    
    private String extractMaterialType(String skillId) {
        String[] parts = skillId.split("\\.");
        if (parts.length >= 3) {
            return parts[2];
        }
        return "unknown";
    }
    
    
    private void updateTreeWithBranches(String rootClusterId, List<TreeDefinition.NodeDefinition> branches) {
        Optional<SkillTree> mainTreeOpt = treeRegistry.getTree("main");
        if (mainTreeOpt.isEmpty()) {
            plugin.getLogger().warning("Main tree not found in registry");
            return;
        }
        
        SkillTree mainTree = mainTreeOpt.get();
        
        plugin.getLogger().info("Dynamic tree update for " + rootClusterId + " would add " + 
            branches.size() + " branches (implementation pending)");
    }
    
    
    public void regenerateAllBranches() {
        List<String> rootClusters = List.of(
            "farmer", "guardsman", "miner", "healer", "blacksmith", "builder", "librarian"
        );
        
        for (String clusterId : rootClusters) {
            generateBranchesForCluster(clusterId);
        }
    }
    
    
    public boolean shouldRegenerateTree(String rootClusterId) {
        Map<String, SkillTreeGenerator.ActionFrequency> currentData = collectActionData(rootClusterId);
        Map<String, SkillTreeGenerator.ActionFrequency> cachedData = actionDataCache.get(rootClusterId);
        
        if (cachedData == null) {
            actionDataCache.put(rootClusterId, currentData);
            return true;
        }
        
        int newActions = 0;
        for (String actionId : currentData.keySet()) {
            if (!cachedData.containsKey(actionId)) {
                newActions++;
            }
        }
        
        double changeRatio = (double) newActions / Math.max(cachedData.size(), 1);
        return changeRatio > 0.2;
    }
    
    private void saveTreeMetadata(String rootClusterId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("lastGenerated", System.currentTimeMillis());
        metadata.put("generationCount", lastGenerationTime.getOrDefault(rootClusterId, 0L));
        metadata.put("actionCount", actionDataCache.getOrDefault(rootClusterId, Map.of()).size());
        
        treeStorage.saveTreeMetadata("branches_" + rootClusterId, metadata);
    }
    
    public Map<String, Object> getTreeStats(String rootClusterId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("clusterId", rootClusterId);
        stats.put("hasSavedBranches", treeStorage.treeExists("branches_" + rootClusterId));
        stats.put("lastGeneration", lastGenerationTime.get(rootClusterId));
        stats.put("cachedActions", actionDataCache.getOrDefault(rootClusterId, Map.of()).size());
        return stats;
    }
    
    public void restructureTreeBasedOnUsage(String rootClusterId) {
        Map<String, SkillTreeGenerator.ActionFrequency> actionData = collectActionData(rootClusterId);
        
        if (actionData.isEmpty()) {
            plugin.getLogger().info("No action data for restructuring: " + rootClusterId);
            return;
        }
        
        treeStorage.loadDynamicBranches(rootClusterId).thenAccept(existingBranchesOpt -> {
            if (existingBranchesOpt.isEmpty()) {
                plugin.getLogger().info("No existing branches to restructure for: " + rootClusterId);
                return;
            }
            
            List<TreeDefinition.NodeDefinition> existingBranches = existingBranchesOpt.get();
            
            Map<String, Long> skillUsageFrequency = calculateSkillUsageFrequency(rootClusterId);
            
            boolean needsRestructure = analyzeIfRestructureNeeded(existingBranches, skillUsageFrequency);
            
            if (needsRestructure) {
                plugin.getLogger().info("Player usage patterns differ from tree structure - restructuring " + rootClusterId);
                
                List<TreeDefinition.NodeDefinition> newBranches = treeGenerator.generateBranches(
                    rootClusterId,
                    actionData,
                    3
                );
                
                updateTreeWithBranches(rootClusterId, newBranches);
                treeStorage.saveDynamicBranches(rootClusterId, newBranches);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("lastRestructure", System.currentTimeMillis());
                metadata.put("reason", "Player usage patterns changed");
                metadata.put("previousBranchCount", existingBranches.size());
                metadata.put("newBranchCount", newBranches.size());
                treeStorage.saveTreeMetadata("branches_" + rootClusterId, metadata);
                
                plugin.getLogger().info("Restructured " + rootClusterId + ": " + 
                    existingBranches.size() + " → " + newBranches.size() + " branches");
            } else {
                plugin.getLogger().info("Tree structure for " + rootClusterId + " matches player usage - no restructure needed");
            }
        });
    }
    
    private Map<String, Long> calculateSkillUsageFrequency(String rootClusterId) {
        Map<String, Long> frequency = new HashMap<>();
        
        for (PlayerSkillData playerData : dataManager.getAllCachedData()) {
            Map<String, Long> skills = playerData.getAllIntrinsicXp();
            
            for (Map.Entry<String, Long> entry : skills.entrySet()) {
                String skillId = entry.getKey();
                
                if (skillId.startsWith(rootClusterId + ".")) {
                    frequency.merge(skillId, entry.getValue(), Long::sum);
                }
            }
        }
        
        return frequency;
    }
    
    private boolean analyzeIfRestructureNeeded(
        List<TreeDefinition.NodeDefinition> existingBranches,
        Map<String, Long> skillUsageFrequency
    ) {
        if (skillUsageFrequency.isEmpty()) {
            return false;
        }
        
        Map<String, Long> existingSkillIds = new HashMap<>();
        collectAllSkillIds(existingBranches, existingSkillIds);
        
        long totalUsage = skillUsageFrequency.values().stream().mapToLong(Long::longValue).sum();
        
        long usageInExistingStructure = 0;
        for (Map.Entry<String, Long> entry : skillUsageFrequency.entrySet()) {
            if (existingSkillIds.containsKey(entry.getKey())) {
                usageInExistingStructure += entry.getValue();
            }
        }
        
        double coverageRatio = totalUsage > 0 ? (double) usageInExistingStructure / totalUsage : 1.0;
        
        int newSkillsNotInTree = 0;
        for (String skillId : skillUsageFrequency.keySet()) {
            if (!existingSkillIds.containsKey(skillId)) {
                newSkillsNotInTree++;
            }
        }
        
        boolean significantNewSkills = newSkillsNotInTree > existingSkillIds.size() * 0.3;
        boolean lowCoverage = coverageRatio < 0.7;
        
        return significantNewSkills || lowCoverage;
    }
    
    private void collectAllSkillIds(List<TreeDefinition.NodeDefinition> nodes, Map<String, Long> skillIds) {
        for (TreeDefinition.NodeDefinition node : nodes) {
            skillIds.put(node.id(), 0L);
            
            if (node.children() != null && !node.children().isEmpty()) {
                collectAllSkillIds(node.children(), skillIds);
            }
        }
    }
    
    public void autoRestructureIfNeeded() {
        List<String> rootClusters = List.of(
            "farmer", "guardsman", "miner", "healer", "blacksmith", "builder", "librarian"
        );
        
        for (String clusterId : rootClusters) {
            if (shouldRegenerateTree(clusterId)) {
                restructureTreeBasedOnUsage(clusterId);
            }
        }
    }
}
