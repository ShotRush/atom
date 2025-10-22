package org.shotrush.atom.ml;

import org.shotrush.atom.model.Models.TreeDefinition;

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
            System.out.println("[SkillTreeGenerator] No action data provided");
            return List.of();
        }
        
        List<ActionFrequency> relevantActions = filterActionsByCluster(rootClusterId, playerActionData);
        
        System.out.println("[SkillTreeGenerator] Filtered " + relevantActions.size() + " relevant actions for " + rootClusterId + 
            " (min required: " + minClusterSize + ")");
        
        if (relevantActions.size() < minClusterSize) {
            System.out.println("[SkillTreeGenerator] Not enough actions to generate branches (need at least " + minClusterSize + ")");
            return List.of();
        }
        
        ClusterNode rootCluster = performHierarchicalClustering(relevantActions, maxDepth);
        
        List<TreeDefinition.NodeDefinition> branches = convertClusterToNodeDefinitions(rootCluster, rootClusterId, 2);
        System.out.println("[SkillTreeGenerator] Generated " + branches.size() + " branch definitions");
        
        return branches;
    }
    
    
    
    private List<ActionFrequency> filterActionsByCluster(String clusterId, Map<String, ActionFrequency> allActions) {
        return allActions.values().stream()
            .filter(action -> isActionRelevantToCluster(action, clusterId))
            .collect(Collectors.toList());
    }
    
    private boolean isActionRelevantToCluster(ActionFrequency action, String clusterId) {
        String material = action.materialType().toLowerCase();
        String actionId = action.actionId().toLowerCase();
        String context = material + "_" + actionId;
        
        return calculateClusterRelevance(context, clusterId) > 0.3;
    }
    
    private double calculateClusterRelevance(String context, String targetCluster) {
        Map<String, Integer> clusterVotes = new HashMap<>();
        String[] knownClusters = {"miner", "farmer", "guardsman", "blacksmith", "builder", "librarian", "healer"};
        
        for (String cluster : knownClusters) {
            double similarity = calculateContextSimilarity(context, cluster);
            if (similarity > 0) {
                clusterVotes.put(cluster, (int)(similarity * 100));
            }
        }
        
        if (clusterVotes.isEmpty()) {
            return inferFromSeeds(context, targetCluster);
        }
        
        int targetVotes = clusterVotes.getOrDefault(targetCluster, 0);
        int totalVotes = clusterVotes.values().stream().mapToInt(Integer::intValue).sum();
        
        return totalVotes > 0 ? (double) targetVotes / totalVotes : 0.0;
    }
    
    private double calculateContextSimilarity(String context, String cluster) {
        String[] contextWords = context.split("[_\\s]+");
        double maxSimilarity = 0.0;
        
        for (String word : contextWords) {
            if (word.length() < 3) continue;
            
            double similarity = calculateWordClusterAffinity(word, cluster);
            maxSimilarity = Math.max(maxSimilarity, similarity);
        }
        
        return maxSimilarity;
    }
    
    private double calculateWordClusterAffinity(String word, String cluster) {
        Map<String, String[]> clusterSeeds = Map.of(
            "miner", new String[]{"ore", "stone", "mine", "coal", "iron", "diamond"},
            "farmer", new String[]{"wheat", "crop", "farm", "cow", "sheep", "breed"},
            "guardsman", new String[]{"zombie", "skeleton", "kill", "combat", "mob"},
            "blacksmith", new String[]{"craft", "forge", "anvil", "sword", "armor"},
            "builder", new String[]{"build", "place", "brick", "wood", "construct"},
            "librarian", new String[]{"enchant", "book", "knowledge", "trade"},
            "healer", new String[]{"potion", "brew", "heal", "medicine"}
        );
        
        String[] seeds = clusterSeeds.getOrDefault(cluster, new String[0]);
        double maxAffinity = 0.0;
        
        for (String seed : seeds) {
            if (word.contains(seed) || seed.contains(word)) {
                maxAffinity = Math.max(maxAffinity, 1.0 - (Math.abs(word.length() - seed.length()) / 10.0));
            }
        }
        
        return maxAffinity;
    }
    
    private double inferFromSeeds(String context, String targetCluster) {
        String lower = context.toLowerCase();
        
        if (targetCluster.equals("miner") && (lower.contains("ore") || lower.contains("stone"))) return 0.8;
        if (targetCluster.equals("farmer") && (lower.contains("crop") || lower.contains("animal"))) return 0.8;
        if (targetCluster.equals("guardsman") && (lower.contains("kill") || lower.contains("mob"))) return 0.8;
        if (targetCluster.equals("blacksmith") && (lower.contains("craft") || lower.contains("forge"))) return 0.8;
        if (targetCluster.equals("builder") && (lower.contains("build") || lower.contains("place"))) return 0.8;
        if (targetCluster.equals("librarian") && (lower.contains("enchant") || lower.contains("book"))) return 0.8;
        if (targetCluster.equals("healer") && (lower.contains("potion") || lower.contains("brew"))) return 0.8;
        
        return 0.0;
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
        
        Map<String, Integer> tokenFrequency = new HashMap<>();
        
        for (ActionFrequency action : actions) {
            String[] tokens = action.materialType().toLowerCase().split("[_\\s]+");
            for (String token : tokens) {
                if (token.length() > 2 && !isCommonWord(token)) {
                    tokenFrequency.merge(token, 1, Integer::sum);
                }
            }
            
            tokens = action.actionId().toLowerCase().split("[_\\s]+");
            for (String token : tokens) {
                if (token.length() > 2 && !isCommonWord(token)) {
                    tokenFrequency.merge(token, 1, Integer::sum);
                }
            }
        }
        
        if (tokenFrequency.isEmpty()) {
            return "Mixed Skills";
        }
        
        List<String> topTokens = tokenFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(2)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        if (topTokens.isEmpty()) {
            return "Mixed Skills";
        }
        
        String clusterName = topTokens.stream()
            .map(this::formatDisplayName)
            .collect(Collectors.joining(" "));
        
        String category = inferCategoryFromTokens(topTokens);
        if (category != null && !category.isEmpty()) {
            return category;
        }
        
        return clusterName;
    }
    
    private boolean isCommonWord(String word) {
        Set<String> commonWords = Set.of("the", "and", "or", "but", "for", "with", "from", 
            "item", "block", "tool", "ore", "type", "use", "get", "set");
        return commonWords.contains(word.toLowerCase());
    }
    
    private String inferCategoryFromTokens(List<String> tokens) {
        String combined = String.join(" ", tokens).toLowerCase();
        
        if (combined.contains("diamond") || combined.contains("emerald") || combined.contains("netherite")) {
            return "Precious Materials";
        }
        if (combined.contains("iron") || combined.contains("gold") || combined.contains("copper")) {
            return "Common Metals";
        }
        if (combined.contains("coal") || combined.contains("redstone") || combined.contains("lapis")) {
            return "Utility Resources";
        }
        if (combined.contains("stone") || combined.contains("andesite") || combined.contains("diorite") || combined.contains("granite")) {
            return "Stone Mining";
        }
        if (combined.contains("wheat") || combined.contains("carrot") || combined.contains("potato")) {
            return "Basic Crops";
        }
        if (combined.contains("cow") || combined.contains("sheep") || combined.contains("pig") || combined.contains("chicken")) {
            return "Animal Husbandry";
        }
        if (combined.contains("sword") || combined.contains("axe") || combined.contains("pickaxe") || combined.contains("shovel")) {
            return "Tool Crafting";
        }
        if (combined.contains("helmet") || combined.contains("chestplate") || combined.contains("leggings") || combined.contains("boots")) {
            return "Armor Crafting";
        }
        if (combined.contains("wood") || combined.contains("plank") || combined.contains("log")) {
            return "Woodworking";
        }
        if (combined.contains("brick") || combined.contains("concrete") || combined.contains("glass")) {
            return "Advanced Building";
        }
        if (combined.contains("zombie") || combined.contains("skeleton") || combined.contains("spider") || combined.contains("creeper")) {
            return "Hostile Mobs";
        }
        if (combined.contains("potion") || combined.contains("brew")) {
            return "Brewing";
        }
        if (combined.contains("enchant") || combined.contains("book")) {
            return "Enchanting";
        }
        
        return null;
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
