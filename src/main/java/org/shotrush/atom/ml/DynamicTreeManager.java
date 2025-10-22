package org.shotrush.atom.ml;

import org.shotrush.atom.Atom;
import org.shotrush.atom.config.TreeBuilder;
import org.shotrush.atom.model.Models.TreeDefinition;
import org.shotrush.atom.manager.PlayerDataManager;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.storage.TreeStorage;
import org.shotrush.atom.tree.Trees.SkillTree;
import org.shotrush.atom.tree.Trees.Registry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public final class DynamicTreeManager {
    
    private final Atom plugin;
    private final Registry treeRegistry;
    private final PlayerDataManager dataManager;
    private final TreeStorage treeStorage;
    private final SkillTreeGenerator treeGenerator;
    private final SpecializationClusterer clusterer;
    private final Map<String, TreeDefinition> dynamicTreeCache;
    private final Map<String, Map<String, SkillTreeGenerator.ActionFrequency>> actionDataCache;
    private final Map<String, Long> lastGenerationTime;
    
    public DynamicTreeManager(
        Atom plugin,
        Registry treeRegistry,
        PlayerDataManager dataManager,
        TreeStorage treeStorage,
        SpecializationClusterer clusterer
    ) {
        this.plugin = plugin;
        this.treeRegistry = treeRegistry;
        this.dataManager = dataManager;
        this.treeStorage = treeStorage;
        this.clusterer = clusterer;
        this.treeGenerator = new SkillTreeGenerator(1, 0.3);
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
            5
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
        
        int playerCount = 0;
        int skillsFound = 0;
        
        for (PlayerSkillData playerData : dataManager.getAllCachedData()) {
            playerCount++;
            Map<String, Long> skills = playerData.getAllIntrinsicXp();
            
            for (Map.Entry<String, Long> entry : skills.entrySet()) {
                String skillId = entry.getKey();
                Long xp = entry.getValue();
                
                if (xp <= 0) continue;
                
                if (skillId.startsWith(rootClusterId + ".") || skillId.equals(rootClusterId)) {
                    skillsFound++;
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
        
        plugin.getLogger().info("[ML Generate] Scanned " + playerCount + " players, found " + 
            skillsFound + " skills for " + rootClusterId + ", created " + aggregatedData.size() + " action frequencies");
        
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
        Optional<org.shotrush.atom.model.SkillNode> rootNodeOpt = mainTree.getNode(rootClusterId);
        
        if (rootNodeOpt.isEmpty()) {
            plugin.getLogger().warning("Root node not found: " + rootClusterId);
            return;
        }
        
        org.shotrush.atom.model.SkillNode rootNode = rootNodeOpt.get();
        int created = 0;
        int skipped = 0;
        
        for (TreeDefinition.NodeDefinition branchDef : branches) {
            String branchId = rootClusterId + "." + branchDef.id();
            
            if (mainTree.getNode(branchId).isPresent()) {
                skipped++;
                continue;
            }
            
            org.shotrush.atom.model.SkillNode newBranch = org.shotrush.atom.model.SkillNode.builder()
                .id(branchId)
                .displayName(branchDef.displayName())
                .parent(rootNode)
                .maxXp(branchDef.maxXp())
                .type(org.shotrush.atom.model.SkillNode.NodeType.BRANCH)
                .build();
            
            rootNode.addChild(newBranch);
            
            if (plugin.getAdvancementGenerator() != null) {
                plugin.getAdvancementGenerator().generateNodeAdvancement(newBranch, rootClusterId);
            }
            
            created++;
            plugin.getLogger().info("  Created branch: " + branchId + " (" + branchDef.displayName() + ")");
        }
        
        if (created > 0) {
            plugin.getLogger().info("Dynamic tree update for " + rootClusterId + ": created " + 
                created + " branches, skipped " + skipped + " existing");
            plugin.getLogger().info("Note: Tree changes are in-memory only. Restart to persist.");
        } else {
            plugin.getLogger().info("Dynamic tree update for " + rootClusterId + ": all " + 
                branches.size() + " branches already exist");
        }
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
    
    public void runSpecializationClustering() {
        plugin.getLogger().info("Running k-means clustering on player specializations...");
        clusterer.performClustering();
        
        List<SpecializationClusterer.Cluster> clusters = clusterer.getClusters();
        plugin.getLogger().info("Discovered " + clusters.size() + " specialization patterns:");
        
        for (int i = 0; i < clusters.size(); i++) {
            SpecializationClusterer.Cluster cluster = clusters.get(i);
            plugin.getLogger().info("  [" + i + "] " + cluster.getLabel() + 
                " (" + cluster.getMembers().size() + " players)");
            plugin.getLogger().info("      Top skills: " + String.join(", ", 
                cluster.getTopSkills().stream().limit(3).toList()));
        }
    }
}
