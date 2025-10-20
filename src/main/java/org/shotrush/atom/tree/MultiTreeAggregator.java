package org.shotrush.atom.tree;

import org.shotrush.atom.model.EffectiveXp;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;
import org.shotrush.atom.progression.DepthProgression;
import org.shotrush.atom.progression.DepthProgression.SpecializationMetrics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MultiTreeAggregator {
    
    private final SkillTreeRegistry registry;
    private final Map<UUID, Map<String, Double>> playerTreeWeights;
    
    public MultiTreeAggregator(SkillTreeRegistry registry) {
        this.registry = registry;
        this.playerTreeWeights = new ConcurrentHashMap<>();
    }
    
    public void updatePlayerWeights(UUID playerId, PlayerSkillData playerData) {
        Map<String, SkillNode> allNodes = buildNodeIndex();
        Map<String, SpecializationMetrics> metrics = DepthProgression.calculateSpecialization(playerData, allNodes);
        Map<String, Double> weights = DepthProgression.calculateTreeWeights(playerData, allNodes, metrics);
        
        playerTreeWeights.put(playerId, weights);
    }
    
    public Map<String, Double> getPlayerWeights(UUID playerId) {
        return playerTreeWeights.getOrDefault(playerId, getDefaultWeights());
    }
    
    private Map<String, Double> getDefaultWeights() {
        Map<String, Double> weights = new HashMap<>();
        int treeCount = registry.treeCount();
        double defaultWeight = treeCount > 0 ? 1.0 / treeCount : 1.0;
        
        for (SkillTree tree : registry.getAllTrees()) {
            weights.put(tree.name(), defaultWeight);
        }
        
        return weights;
    }
    
    public EffectiveXp aggregateXp(UUID playerId, String skillId, 
                                   Map<SkillTree, EffectiveXp> treeXpValues) {
        
        Map<String, Double> weights = getPlayerWeights(playerId);
        
        double totalWeight = 0.0;
        long weightedIntrinsic = 0;
        long weightedHonorary = 0;
        int maxXp = 0;
        
        for (Map.Entry<SkillTree, EffectiveXp> entry : treeXpValues.entrySet()) {
            SkillTree tree = entry.getKey();
            EffectiveXp xp = entry.getValue();
            
            double weight = weights.getOrDefault(tree.name(), tree.weight());
            
            weightedIntrinsic += (long) (xp.intrinsicXp() * weight);
            weightedHonorary += (long) (xp.honoraryXp() * weight);
            totalWeight += weight;
            
            if (maxXp == 0) {
                maxXp = (int) (xp.intrinsicXp() + xp.honoraryXp());
            }
        }
        
        if (totalWeight == 0.0) {
            return EffectiveXp.zero();
        }
        
        long finalIntrinsic = (long) (weightedIntrinsic / totalWeight);
        long finalHonorary = (long) (weightedHonorary / totalWeight);
        
        return EffectiveXp.of(finalIntrinsic, finalHonorary, maxXp);
    }
    
    public void clearPlayerWeights(UUID playerId) {
        playerTreeWeights.remove(playerId);
    }
    
    public Map<String, TreeInfluence> analyzeTreeInfluence(UUID playerId, PlayerSkillData playerData) {
        Map<String, Double> weights = getPlayerWeights(playerId);
        Map<String, SkillNode> allNodes = buildNodeIndex();
        Map<String, SpecializationMetrics> metrics = DepthProgression.calculateSpecialization(playerData, allNodes);
        
        Map<String, TreeInfluence> influence = new HashMap<>();
        
        for (SkillTree tree : registry.getAllTrees()) {
            String treeName = tree.name();
            double weight = weights.getOrDefault(treeName, tree.weight());
            SpecializationMetrics treeMetrics = metrics.get(treeName);
            
            if (treeMetrics != null) {
                influence.put(treeName, new TreeInfluence(
                    weight,
                    treeMetrics.specializationScore(),
                    treeMetrics.maxDepth(),
                    treeMetrics.breadth()
                ));
            }
        }
        
        return influence;
    }
    
    private Map<String, SkillNode> buildNodeIndex() {
        Map<String, SkillNode> allNodes = new HashMap<>();
        
        for (SkillTree tree : registry.getAllTrees()) {
            for (String skillId : tree.getAllSkillIds()) {
                tree.getNode(skillId).ifPresent(node -> allNodes.putIfAbsent(skillId, node));
            }
        }
        
        return allNodes;
    }
    
    public record TreeInfluence(
        double weight,
        double specializationScore,
        int maxDepth,
        int breadth
    ) {}
}
