package org.shotrush.atom.progression;

import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;

import java.util.*;


public final class DepthProgression {
    
    
    public static int calculateEffectiveDepth(SkillNode node, PlayerSkillData playerData) {
        int depth = node.depth();
        double xpRatio = getXpRatio(node, playerData);
        
        
        if (xpRatio < 0.25) {
            return Math.max(0, depth - 1);
        }
        
        return depth;
    }
    
    
    public static Map<String, SpecializationMetrics> calculateSpecialization(
            PlayerSkillData playerData,
            Map<String, SkillNode> allNodes) {
        
        Map<String, SpecializationMetrics> metrics = new HashMap<>();
        Map<String, List<NodeProgress>> treeProgress = groupByTree(playerData, allNodes);
        
        for (Map.Entry<String, List<NodeProgress>> entry : treeProgress.entrySet()) {
            String treeName = entry.getKey();
            List<NodeProgress> progress = entry.getValue();
            
            metrics.put(treeName, calculateTreeSpecialization(progress));
        }
        
        return metrics;
    }
    
    public static Map<String, Double> calculateTreeWeights(
            PlayerSkillData playerData,
            Map<String, SkillNode> allNodes,
            Map<String, SpecializationMetrics> metrics) {
        
        Map<String, Double> weights = new HashMap<>();
        Map<String, List<NodeProgress>> treeProgress = groupByTree(playerData, allNodes);
        
        double totalActivity = 0.0;
        Map<String, Double> treeActivity = new HashMap<>();
        
        for (Map.Entry<String, List<NodeProgress>> entry : treeProgress.entrySet()) {
            String treeName = entry.getKey();
            double activity = calculateTreeActivity(entry.getValue(), metrics.get(treeName));
            treeActivity.put(treeName, activity);
            totalActivity += activity;
        }
        
        if (totalActivity == 0.0) {
            for (String treeName : treeProgress.keySet()) {
                weights.put(treeName, 1.0 / treeProgress.size());
            }
            return weights;
        }
        
        for (Map.Entry<String, Double> entry : treeActivity.entrySet()) {
            double normalizedWeight = entry.getValue() / totalActivity;
            double adaptiveWeight = 0.5 + (normalizedWeight * 0.5);
            weights.put(entry.getKey(), adaptiveWeight);
            
            if (adaptiveWeight > 0.75) {
                System.out.println("[ML] High tree weight detected: " + entry.getKey() + " = " + 
                    String.format("%.2f", adaptiveWeight) + " (activity: " + String.format("%.0f", entry.getValue()) + ")");
            }
        }
        
        return weights;
    }
    
    private static double calculateTreeActivity(List<NodeProgress> progress, SpecializationMetrics metrics) {
        if (progress.isEmpty() || metrics == null) return 0.0;
        
        double totalXp = progress.stream().mapToLong(p -> p.xp).sum();
        double depthFactor = metrics.averageDepth() / 5.0;
        double specializationFactor = metrics.specializationScore();
        
        return totalXp * (1.0 + depthFactor + specializationFactor);
    }
    
    
    private static Map<String, List<NodeProgress>> groupByTree(
            PlayerSkillData playerData,
            Map<String, SkillNode> allNodes) {
        
        Map<String, List<NodeProgress>> grouped = new HashMap<>();
        Map<String, List<NodeProgress>> dynamic = new HashMap<>();
        
        for (Map.Entry<String, Long> entry : playerData.getAllIntrinsicXp().entrySet()) {
            String skillId = entry.getKey();
            long xp = entry.getValue();
            
            SkillNode node = allNodes.get(skillId);
            if (node == null) continue;
            
            if (isDynamicNode(skillId, xp, allNodes)) {
                String cluster = findDynamicCluster(skillId, xp, playerData, allNodes);
                dynamic.computeIfAbsent(cluster, k -> new ArrayList<>())
                       .add(new NodeProgress(node, xp));
            } else {
                String treeName = extractTreeName(skillId);
                grouped.computeIfAbsent(treeName, k -> new ArrayList<>())
                       .add(new NodeProgress(node, xp));
            }
        }
        
        grouped.putAll(dynamic);
        return grouped;
    }
    
    
    private static String extractTreeName(String skillId) {
        int firstDot = skillId.indexOf('.');
        return firstDot > 0 ? skillId.substring(0, firstDot) : skillId;
    }
    
    private static boolean isDynamicNode(String skillId, long xp, Map<String, SkillNode> allNodes) {
        SkillNode node = allNodes.get(skillId);
        if (node == null) return false;
        
        double depthRatio = node.depth() / 5.0;
        double xpThreshold = node.maxXp() * (1.5 + depthRatio);
        
        return xp > xpThreshold && node.depth() >= 2;
    }
    
    private static String findDynamicCluster(String skillId, long xp, PlayerSkillData data, Map<String, SkillNode> allNodes) {
        SkillNode node = allNodes.get(skillId);
        if (node == null) return extractTreeName(skillId);
        
        Map<String, Double> affinities = calculateAffinities(node, data, allNodes);
        
        String bestCluster = null;
        double maxAffinity = 0.0;
        
        for (Map.Entry<String, Double> entry : affinities.entrySet()) {
            if (entry.getValue() > maxAffinity && entry.getValue() > 0.6) {
                maxAffinity = entry.getValue();
                bestCluster = entry.getKey();
            }
        }
        
        if (bestCluster != null) {
            System.out.println("[ML] Dynamic cluster formed: '" + bestCluster + "' for skill '" + skillId + 
                "' (affinity: " + String.format("%.2f", maxAffinity) + ", xp: " + xp + ")");
        }
        
        return bestCluster != null ? bestCluster : extractTreeName(skillId);
    }
    
    private static Map<String, Double> calculateAffinities(SkillNode node, PlayerSkillData data, Map<String, SkillNode> allNodes) {
        Map<String, Double> affinities = new HashMap<>();
        Map<String, List<SkillNode>> potentialClusters = new HashMap<>();
        
        for (SkillNode other : allNodes.values()) {
            long otherXp = data.getAllIntrinsicXp().getOrDefault(other.id(), 0L);
            if (otherXp < other.maxXp() * 1.2) continue;
            
            double similarity = calculateSimilarity(node, other, data);
            if (similarity > 0.5) {
                String clusterKey = generateClusterKey(node, other);
                potentialClusters.computeIfAbsent(clusterKey, k -> new ArrayList<>()).add(other);
            }
        }
        
        for (Map.Entry<String, List<SkillNode>> entry : potentialClusters.entrySet()) {
            if (entry.getValue().size() >= 2) {
                double affinity = entry.getValue().stream()
                    .mapToDouble(n -> calculateSimilarity(node, n, data))
                    .average()
                    .orElse(0.0);
                affinities.put(entry.getKey(), affinity);
            }
        }
        
        return affinities;
    }
    
    private static double calculateSimilarity(SkillNode n1, SkillNode n2, PlayerSkillData data) {
        double depthSim = 1.0 - Math.abs(n1.depth() - n2.depth()) / 5.0;
        
        long xp1 = data.getAllIntrinsicXp().getOrDefault(n1.id(), 0L);
        long xp2 = data.getAllIntrinsicXp().getOrDefault(n2.id(), 0L);
        double xpSim = Math.min(xp1, xp2) / (double) Math.max(xp1, xp2);
        
        String[] parts1 = n1.id().split("\\.");
        String[] parts2 = n2.id().split("\\.");
        int commonParts = 0;
        for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
            if (parts1[i].equals(parts2[i])) commonParts++;
            else break;
        }
        double pathSim = commonParts / (double) Math.max(parts1.length, parts2.length);
        
        return (depthSim * 0.3) + (xpSim * 0.4) + (pathSim * 0.3);
    }
    
    private static String generateClusterKey(SkillNode n1, SkillNode n2) {
        String[] parts1 = n1.id().split("\\.");
        String[] parts2 = n2.id().split("\\.");
        
        StringBuilder key = new StringBuilder("dynamic_");
        for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
            if (parts1[i].equals(parts2[i])) {
                key.append(parts1[i]).append("_");
            } else {
                break;
            }
        }
        
        if (key.length() == 8) {
            key.append(Math.max(n1.depth(), n2.depth()));
        }
        
        return key.toString();
    }
    
    
    private static SpecializationMetrics calculateTreeSpecialization(List<NodeProgress> progress) {
        if (progress.isEmpty()) {
            return new SpecializationMetrics(0, 0, 0.0, 0.0);
        }
        
        
        int maxDepth = progress.stream()
                .mapToInt(p -> p.node.depth())
                .max()
                .orElse(0);
        
        
        long breadth = progress.stream()
                .filter(p -> p.node.depth() == 1)
                .count();
        
        
        double totalXp = progress.stream().mapToLong(p -> p.xp).sum();
        double weightedDepth = progress.stream()
                .mapToDouble(p -> p.node.depth() * (p.xp / totalXp))
                .sum();
        
        
        
        double specializationScore = Math.min(1.0, maxDepth / (breadth + 1.0) / 5.0);
        
        return new SpecializationMetrics(maxDepth, (int) breadth, weightedDepth, specializationScore);
    }
    
    
    private static double getXpRatio(SkillNode node, PlayerSkillData playerData) {
        long currentXp = playerData.getAllIntrinsicXp().getOrDefault(node.id(), 0L);
        return (double) currentXp / node.maxXp();
    }
    
    
    public static double calculatePenaltyMultiplier(
            SkillNode node,
            PlayerSkillData playerData,
            SpecializationMetrics metrics) {
        
        int nodeDepth = node.depth();
        double specializationScore = metrics.specializationScore();
        
        
        if (specializationScore < 0.3) {
            
            return 1.0 - (nodeDepth * 0.15); 
        }
        
        
        if (specializationScore > 0.7) {
            
            boolean inSpecialization = isInSpecializationPath(node, playerData);
            
            if (!inSpecialization) {
                
                return 0.5; 
            }
        }
        
        
        return 1.0;
    }
    
    
    public static double calculateBonusMultiplier(
            SkillNode node,
            PlayerSkillData playerData,
            SpecializationMetrics metrics) {
        
        int nodeDepth = node.depth();
        double specializationScore = metrics.specializationScore();
        double xpRatio = getXpRatio(node, playerData);
        
        double baseBonus = xpRatio * 0.5;
        double depthBonus = nodeDepth * 0.1 * specializationScore;
        double specBonus = specializationScore * 0.3;
        
        long xp = playerData.getAllIntrinsicXp().getOrDefault(node.id(), 0L);
        double xpRatioBonus = Math.min(1.0, (xp / (double) node.maxXp()) - 1.5);
        if (xpRatioBonus > 0 && nodeDepth >= 2) {
            double hyperBonus = xpRatioBonus * 2.0;
            double totalMultiplier = 1.0 + baseBonus + depthBonus + specBonus + hyperBonus;
            
            if (hyperBonus > 0.5) {
                System.out.println("[ML] Hyperspecialization bonus: " + node.id() + " = " + 
                    String.format("%.2fx", totalMultiplier) + " (hyper: +" + String.format("%.0f%%", hyperBonus * 100) + ")");
            }
            
            return totalMultiplier;
        }
        
        return 1.0 + baseBonus + depthBonus + specBonus;
    }
    
    
    private static boolean isInSpecializationPath(SkillNode node, PlayerSkillData playerData) {
        
        List<SkillNode> ancestors = node.ancestors();
        
        long pathXp = playerData.getAllIntrinsicXp().getOrDefault(node.id(), 0L);
        for (SkillNode ancestor : ancestors) {
            pathXp += playerData.getAllIntrinsicXp().getOrDefault(ancestor.id(), 0L);
        }
        
        
        return pathXp > 10000; 
    }
    
    
    private record NodeProgress(SkillNode node, long xp) {}
    
    
    public record SpecializationMetrics(
            int maxDepth,
            int breadth,
            double averageDepth,
            double specializationScore
    ) {}
}
