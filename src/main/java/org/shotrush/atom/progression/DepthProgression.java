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
    
    
    private static Map<String, List<NodeProgress>> groupByTree(
            PlayerSkillData playerData,
            Map<String, SkillNode> allNodes) {
        
        Map<String, List<NodeProgress>> grouped = new HashMap<>();
        
        for (Map.Entry<String, Long> entry : playerData.getAllIntrinsicXp().entrySet()) {
            String skillId = entry.getKey();
            long xp = entry.getValue();
            
            SkillNode node = allNodes.get(skillId);
            if (node == null) continue;
            
            String treeName = extractTreeName(skillId);
            grouped.computeIfAbsent(treeName, k -> new ArrayList<>())
                   .add(new NodeProgress(node, xp));
        }
        
        return grouped;
    }
    
    
    private static String extractTreeName(String skillId) {
        int firstDot = skillId.indexOf('.');
        return firstDot > 0 ? skillId.substring(0, firstDot) : skillId;
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
        
        return 1.0 + baseBonus + depthBonus + specBonus;
    }
    
    
    private static boolean isInSpecializationPath(SkillNode node, PlayerSkillData playerData) {
        
        List<SkillNode> ancestors = node.ancestors();
        
        double pathXp = playerData.getAllIntrinsicXp().getOrDefault(node.id(), 0L);
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
