package org.shotrush.atom.ml;

import org.shotrush.atom.config.TreeDefinition;

import java.util.*;
import java.util.stream.Collectors;


public final class SkillTreeGenerator {
    
    private final int minClusterSize;
    private final double similarityThreshold;
    
    public SkillTreeGenerator(int minClusterSize, double similarityThreshold) {
        this.minClusterSize = minClusterSize;
        this.similarityThreshold = similarityThreshold;
    }
    
    
    public List<TreeDefinition.NodeDefinition> generateBranches(
        String rootClusterId,
        Map<String, ActionFrequency> playerActionData,
        int maxDepth
    ) {
        if (playerActionData.isEmpty()) {
            return List.of();
        }
        
        List<ActionFrequency> relevantActions = filterActionsByCluster(rootClusterId, playerActionData);
        
        if (relevantActions.size() < minClusterSize) {
            return List.of();
        }
        
        ClusterNode rootCluster = performHierarchicalClustering(relevantActions, maxDepth);
        
        return convertClusterToNodeDefinitions(rootCluster, rootClusterId, 2);
    }
    
    
    
    private List<ActionFrequency> filterActionsByCluster(String clusterId, Map<String, ActionFrequency> allActions) {
        return allActions.values().stream()
            .filter(action -> isActionRelevantToCluster(action, clusterId))
            .collect(Collectors.toList());
    }
    
    private boolean isActionRelevantToCluster(ActionFrequency action, String clusterId) {
        String material = action.materialType().toLowerCase();
        String actionId = action.actionId().toLowerCase();
        
        return switch (clusterId) {
            case "miner" -> isMiningAction(material, actionId);
            case "farmer" -> isFarmingAction(material, actionId);
            case "guardsman" -> isCombatAction(material, actionId);
            case "blacksmith" -> isCraftingAction(material, actionId);
            case "builder" -> isBuildingAction(material, actionId);
            case "healer" -> isHealingAction(material, actionId);
            case "librarian" -> isKnowledgeAction(material, actionId);
            default -> false;
        };
    }
    
    private boolean isMiningAction(String material, String actionId) {
        return material.contains("ore") || material.contains("stone") || 
               material.contains("andesite") || material.contains("diorite") || 
               material.contains("granite") || actionId.contains("mine");
    }
    
    private boolean isFarmingAction(String material, String actionId) {
        return material.contains("wheat") || material.contains("carrot") || 
               material.contains("potato") || material.contains("beetroot") ||
               material.contains("cow") || material.contains("sheep") ||
               material.contains("pig") || material.contains("chicken") ||
               actionId.contains("farm") || actionId.contains("breed") || actionId.contains("plant");
    }
    
    private boolean isCombatAction(String material, String actionId) {
        return material.contains("zombie") || material.contains("skeleton") ||
               material.contains("spider") || material.contains("creeper") ||
               actionId.contains("kill") || actionId.contains("combat");
    }
    
    private boolean isCraftingAction(String material, String actionId) {
        return material.contains("pickaxe") || material.contains("axe") ||
               material.contains("shovel") || material.contains("hoe") ||
               material.contains("helmet") || material.contains("chestplate") ||
               material.contains("leggings") || material.contains("boots") ||
               actionId.contains("craft");
    }
    
    private boolean isBuildingAction(String material, String actionId) {
        return material.contains("dirt") || material.contains("wood") ||
               material.contains("cobblestone") || material.contains("planks") ||
               material.contains("bricks") || material.contains("quartz") ||
               actionId.contains("place") || actionId.contains("build");
    }
    
    private boolean isHealingAction(String material, String actionId) {
        return material.contains("potion") || actionId.contains("brew") || actionId.contains("heal");
    }
    
    private boolean isKnowledgeAction(String material, String actionId) {
        return material.contains("book") || material.contains("enchant") || actionId.contains("enchant");
    }
    
    
    
    private ClusterNode performHierarchicalClustering(List<ActionFrequency> actions, int maxDepth) {
        List<ClusterNode> clusters = actions.stream()
            .map(action -> new ClusterNode(action, new ArrayList<>()))
            .collect(Collectors.toList());
        
        int currentDepth = 0;
        
        while (clusters.size() > 1 && currentDepth < maxDepth) {
            MergePair closestPair = findClosestClusters(clusters);
            
            if (closestPair.distance() > similarityThreshold) {
                break;
            }
            
            ClusterNode merged = mergeClusters(closestPair.cluster1(), closestPair.cluster2());
            clusters.remove(closestPair.cluster1());
            clusters.remove(closestPair.cluster2());
            clusters.add(merged);
            
            currentDepth++;
        }
        
        if (clusters.size() == 1) {
            return clusters.get(0);
        }
        
        return new ClusterNode(null, new ArrayList<>(clusters));
    }
    
    private MergePair findClosestClusters(List<ClusterNode> clusters) {
        double minDistance = Double.MAX_VALUE;
        ClusterNode closest1 = null;
        ClusterNode closest2 = null;
        
        for (int i = 0; i < clusters.size(); i++) {
            for (int j = i + 1; j < clusters.size(); j++) {
                double distance = calculateClusterDistance(clusters.get(i), clusters.get(j));
                if (distance < minDistance) {
                    minDistance = distance;
                    closest1 = clusters.get(i);
                    closest2 = clusters.get(j);
                }
            }
        }
        
        return new MergePair(closest1, closest2, minDistance);
    }
    
    private double calculateClusterDistance(ClusterNode c1, ClusterNode c2) {
        List<ActionFrequency> actions1 = c1.getAllActions();
        List<ActionFrequency> actions2 = c2.getAllActions();
        
        double totalDistance = 0;
        int count = 0;
        
        for (ActionFrequency a1 : actions1) {
            for (ActionFrequency a2 : actions2) {
                totalDistance += calculateActionDistance(a1, a2);
                count++;
            }
        }
        
        return count > 0 ? totalDistance / count : Double.MAX_VALUE;
    }
    
    private ClusterNode mergeClusters(ClusterNode c1, ClusterNode c2) {
        List<ClusterNode> children = new ArrayList<>();
        children.add(c1);
        children.add(c2);
        return new ClusterNode(null, children);
    }
    
    
    
    
    private double calculateActionDistance(ActionFrequency action1, ActionFrequency action2) {
        double materialSimilarity = calculateMaterialSimilarity(action1.materialType(), action2.materialType());
        double xpSimilarity = calculateXpSimilarity(action1.averageXpPerAction(), action2.averageXpPerAction());
        double frequencySimilarity = calculateFrequencySimilarity(action1.totalOccurrences(), action2.totalOccurrences());
        
        return (materialSimilarity * 0.5) + (xpSimilarity * 0.3) + (frequencySimilarity * 0.2);
    }
    
    private double calculateMaterialSimilarity(String material1, String material2) {
        if (material1.equals(material2)) {
            return 0.0;
        }
        
        Set<String> tokens1 = tokenize(material1);
        Set<String> tokens2 = tokenize(material2);
        
        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);
        
        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);
        
        if (union.isEmpty()) {
            return 1.0;
        }
        
        double jaccardSimilarity = (double) intersection.size() / union.size();
        return 1.0 - jaccardSimilarity;
    }
    
    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        String[] parts = text.toLowerCase().split("[_\\s]+");
        for (String part : parts) {
            tokens.add(part);
            if (part.length() > 3) {
                for (int i = 0; i <= part.length() - 3; i++) {
                    tokens.add(part.substring(i, i + 3));
                }
            }
        }
        return tokens;
    }
    
    private double calculateXpSimilarity(double xp1, double xp2) {
        if (xp1 == 0 && xp2 == 0) {
            return 0.0;
        }
        
        double maxXp = Math.max(xp1, xp2);
        double minXp = Math.min(xp1, xp2);
        
        if (maxXp == 0) {
            return 1.0;
        }
        
        return 1.0 - (minXp / maxXp);
    }
    
    private double calculateFrequencySimilarity(long freq1, long freq2) {
        if (freq1 == 0 && freq2 == 0) {
            return 0.0;
        }
        
        long maxFreq = Math.max(freq1, freq2);
        long minFreq = Math.min(freq1, freq2);
        
        if (maxFreq == 0) {
            return 1.0;
        }
        
        double ratio = (double) minFreq / maxFreq;
        return 1.0 - ratio;
    }
    
    
    
    private List<TreeDefinition.NodeDefinition> convertClusterToNodeDefinitions(
        ClusterNode cluster,
        String parentId,
        int currentDepth
    ) {
        List<TreeDefinition.NodeDefinition> nodes = new ArrayList<>();
        
        if (cluster.action() != null) {
            String nodeId = parentId + "." + sanitizeId(cluster.action().actionId());
            String displayName = formatDisplayName(cluster.action().actionId());
            int xp = calculateXpRequirement(currentDepth);
            
            nodes.add(new TreeDefinition.NodeDefinition(nodeId, displayName, xp, "LEAF", null));
        } else if (!cluster.children().isEmpty()) {
            for (int i = 0; i < cluster.children().size(); i++) {
                ClusterNode child = cluster.children().get(i);
                String branchId = parentId + ".branch_" + i;
                String branchName = generateBranchName(child);
                int xp = calculateXpRequirement(currentDepth);
                
                List<TreeDefinition.NodeDefinition> childNodes = convertClusterToNodeDefinitions(
                    child, branchId, currentDepth + 1
                );
                
                String nodeType = childNodes.isEmpty() ? "LEAF" : "BRANCH";
                nodes.add(new TreeDefinition.NodeDefinition(branchId, branchName, xp, nodeType, childNodes));
            }
        }
        
        return nodes;
    }
    
    private String sanitizeId(String id) {
        return id.toLowerCase()
            .replaceAll("[^a-z0-9_]", "_")
            .replaceAll("_+", "_");
    }
    
    private String formatDisplayName(String id) {
        return Arrays.stream(id.split("[_\\s]+"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }
    
    private String generateBranchName(ClusterNode cluster) {
        List<ActionFrequency> actions = cluster.getAllActions();
        if (actions.isEmpty()) {
            return "Unknown Branch";
        }
        
        Map<String, Long> materialCounts = actions.stream()
            .collect(Collectors.groupingBy(ActionFrequency::materialType, Collectors.counting()));
        
        String mostCommon = materialCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Mixed");
        
        return formatDisplayName(mostCommon) + " Skills";
    }
    
    private int calculateXpRequirement(int depth) {
        return switch (depth) {
            case 2 -> 5000;
            case 3 -> 10000;
            case 4 -> 15000;
            default -> 20000;
        };
    }
    
    
    
    
    public record ActionFrequency(
        String actionId,
        String materialType,
        long totalOccurrences,
        double averageXpPerAction,
        Map<String, Object> metadata
    ) {}
    
    private record ClusterNode(ActionFrequency action, List<ClusterNode> children) {
        List<ActionFrequency> getAllActions() {
            List<ActionFrequency> actions = new ArrayList<>();
            if (action != null) {
                actions.add(action);
            }
            for (ClusterNode child : children) {
                actions.addAll(child.getAllActions());
            }
            return actions;
        }
    }
    
    private record MergePair(ClusterNode cluster1, ClusterNode cluster2, double distance) {}
}
