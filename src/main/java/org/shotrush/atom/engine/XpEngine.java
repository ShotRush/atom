package org.shotrush.atom.engine;

import org.shotrush.atom.model.EffectiveXp;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;
import org.shotrush.atom.tree.SkillTree;
import org.shotrush.atom.tree.SkillTreeRegistry;

import java.util.*;

public final class XpEngine {
    
    private final SkillTreeRegistry treeRegistry;
    private final XpCalculator calculator;
    
    public XpEngine(SkillTreeRegistry treeRegistry) {
        this.treeRegistry = Objects.requireNonNull(treeRegistry, "treeRegistry cannot be null");
        this.calculator = new XpCalculator();
    }
    
    public void awardXp(PlayerSkillData playerData, String skillId, long amount) {
        Objects.requireNonNull(playerData, "playerData cannot be null");
        Objects.requireNonNull(skillId, "skillId cannot be null");
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        Optional<SkillNode> nodeOpt = treeRegistry.findNode(skillId);
        if (nodeOpt.isEmpty()) {
            return;
        }
        
        SkillNode node = nodeOpt.get();
        long currentXp = playerData.getIntrinsicXp(skillId);
        long newXp = Math.min(currentXp + amount, node.maxXp());
        
        playerData.setIntrinsicXp(skillId, newXp);
        calculator.invalidateCache(playerData.playerId());
    }
    
    public void setXp(PlayerSkillData playerData, String skillId, long amount) {
        Objects.requireNonNull(playerData, "playerData cannot be null");
        Objects.requireNonNull(skillId, "skillId cannot be null");
        
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        
        Optional<SkillNode> nodeOpt = treeRegistry.findNode(skillId);
        if (nodeOpt.isEmpty()) {
            return;
        }
        
        SkillNode node = nodeOpt.get();
        long cappedXp = Math.min(amount, node.maxXp());
        
        playerData.setIntrinsicXp(skillId, cappedXp);
        calculator.invalidateCache(playerData.playerId());
    }
    
    public EffectiveXp getEffectiveXp(PlayerSkillData playerData, String skillId) {
        Objects.requireNonNull(playerData, "playerData cannot be null");
        Objects.requireNonNull(skillId, "skillId cannot be null");
        
        Optional<SkillNode> nodeOpt = treeRegistry.findNode(skillId);
        if (nodeOpt.isEmpty()) {
            return EffectiveXp.zero();
        }
        
        return calculator.calculateEffectiveXp(playerData, nodeOpt.get());
    }
    
    public Map<String, EffectiveXp> getAllEffectiveXp(PlayerSkillData playerData, SkillTree tree) {
        Objects.requireNonNull(playerData, "playerData cannot be null");
        Objects.requireNonNull(tree, "tree cannot be null");
        
        Map<String, SkillNode> nodes = new HashMap<>();
        collectNodes(tree.root(), nodes);
        
        return calculator.calculateAllEffectiveXp(playerData, nodes);
    }
    
    private void collectNodes(SkillNode node, Map<String, SkillNode> accumulator) {
        accumulator.put(node.id(), node);
        for (SkillNode child : node.children().values()) {
            collectNodes(child, accumulator);
        }
    }
    
    public double getSkillLevel(PlayerSkillData playerData, String skillId) {
        EffectiveXp effectiveXp = getEffectiveXp(playerData, skillId);
        return effectiveXp.progressPercent() * 100.0;
    }
    
    public boolean hasReachedThreshold(PlayerSkillData playerData, String skillId, double thresholdPercent) {
        if (thresholdPercent < 0.0 || thresholdPercent > 100.0) {
            throw new IllegalArgumentException("Threshold must be between 0 and 100");
        }
        
        double level = getSkillLevel(playerData, skillId);
        return level >= thresholdPercent;
    }
    
    public List<String> getSkillsAboveThreshold(PlayerSkillData playerData, double thresholdPercent) {
        List<String> skills = new ArrayList<>();
        
        for (SkillTree tree : treeRegistry.getAllTrees()) {
            for (String skillId : tree.getAllSkillIds()) {
                if (hasReachedThreshold(playerData, skillId, thresholdPercent)) {
                    skills.add(skillId);
                }
            }
        }
        
        return skills;
    }
    
    public XpCalculator calculator() {
        return calculator;
    }
}
