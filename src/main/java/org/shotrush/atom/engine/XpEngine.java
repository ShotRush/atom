package org.shotrush.atom.engine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.shotrush.atom.model.Models.EffectiveXp;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;
import org.shotrush.atom.tree.Trees;
import org.shotrush.atom.tree.Trees.SkillTree;
import org.shotrush.atom.tree.Trees.Registry;
import java.util.*;

import static org.shotrush.atom.engine.XpCalculator.*;

public final class XpEngine {
    
    private final Registry treeRegistry;
    private final Trees.Aggregator aggregator;
    private final org.shotrush.atom.config.AtomConfig config;
    private final XpCalculator calculator;
    private org.shotrush.atom.advancement.AdvancementGenerator advancementGenerator;
    
    public XpEngine(Registry treeRegistry, Trees.Aggregator aggregator, org.shotrush.atom.config.AtomConfig config) {
        this.treeRegistry = Objects.requireNonNull(treeRegistry, "treeRegistry cannot be null");
        this.aggregator = aggregator;
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.calculator = new XpCalculator();
    }
    
    public void setAdvancementGenerator(org.shotrush.atom.advancement.AdvancementGenerator generator) {
        this.advancementGenerator = generator;
    }
    
    public void awardXp(PlayerSkillData playerData, String skillId, long amount) {
        Objects.requireNonNull(playerData, "playerData cannot be null");
        Objects.requireNonNull(skillId, "skillId cannot be null");
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        Optional<SkillNode> nodeOpt = treeRegistry.findNode(skillId);
        if (nodeOpt.isEmpty()) {
            playerData.addIntrinsicXp(skillId, amount);
            
            String rootCategory = extractRootCategory(skillId);
            Optional<SkillNode> rootNodeOpt = treeRegistry.findNode(rootCategory);
            
            if (rootNodeOpt.isPresent()) {
                SkillNode rootNode = rootNodeOpt.get();
                
                createDynamicNodeHierarchy(skillId, rootNode);
                
                propagateXpTopDown(playerData, rootNode, amount);
                
                if (aggregator != null) {
                    aggregator.updatePlayerWeights(playerData.playerId(), playerData);
                }
            } else {
                playerData.addIntrinsicXp(rootCategory, amount);
            }
            
            calculator.invalidateCache(playerData.playerId());
            
            if (config.enableFeedback()) {
                showXpFeedback(playerData.playerId(), amount, skillId);
            }
            return;
        }
        
        SkillNode node = nodeOpt.get();
        
        propagateXpTopDown(playerData, node, amount);
        
        calculator.invalidateCache(playerData.playerId());
        
        if (aggregator != null) {
            aggregator.updatePlayerWeights(playerData.playerId(), playerData);
        }
        
        if (config.enableFeedback()) {
            showXpFeedback(playerData.playerId(), amount, node.id());
        }
    }
    
    private String extractRootCategory(String skillId) {
        int firstDot = skillId.indexOf('.');
        if (firstDot == -1) {
            return skillId;
        }
        return skillId.substring(0, firstDot);
    }
    
    private void createDynamicNodeHierarchy(String skillId, SkillNode rootNode) {
        String[] parts = skillId.split("\\.");
        if (parts.length < 2) return;
        
        SkillNode currentParent = rootNode;
        StringBuilder currentPath = new StringBuilder(parts[0]);
        
        for (int i = 1; i < parts.length; i++) {
            currentPath.append(".").append(parts[i]);
            String nodeId = currentPath.toString();
            
            Optional<SkillNode> existingNode = treeRegistry.findNode(nodeId);
            if (existingNode.isPresent()) {
                currentParent = existingNode.get();
            } else {
                String displayName = formatDisplayName(parts[i]);
                int depth = i + 1;
                int maxXp = calculateDynamicMaxXp(depth);
                
                SkillNode newNode = SkillNode.builder()
                    .id(nodeId)
                    .displayName(displayName)
                    .parent(currentParent)
                    .maxXp(maxXp)
                    .type(i == parts.length - 1 ? SkillNode.NodeType.LEAF : SkillNode.NodeType.BRANCH)
                    .build();
                
                currentParent.addChild(newNode);
                
                if (advancementGenerator != null) {
                    advancementGenerator.generateNodeAdvancement(newNode, currentParent.id());
                }
                
                currentParent = newNode;
                
                System.out.println("[Dynamic Node] Created: " + nodeId + 
                    " (depth: " + depth + ", maxXp: " + maxXp + ", type: " + newNode.type() + ")");
            }
        }
    }
    
    private String formatDisplayName(String part) {
        return java.util.Arrays.stream(part.split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(java.util.stream.Collectors.joining(" "));
    }
    
    private int calculateDynamicMaxXp(int depth) {
        return switch (depth) {
            case 1 -> 1000;
            case 2 -> 5000;
            case 3 -> 10000;
            case 4 -> 15000;
            case 5 -> 25000;
            case 6 -> 40000;
            case 7 -> 60000;
            case 8 -> 100000;
            default -> 150000;
        };
    }
    
    private void showXpFeedback(UUID playerId, long amount, String skillName) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        
        Component message = Component.text("+" + amount + " ", NamedTextColor.GRAY)
            .append(Component.text(skillName, NamedTextColor.WHITE));
        
        player.sendActionBar(message);
    }
    
    private void propagateXpTopDown(PlayerSkillData playerData, SkillNode node, long amount) {
        List<SkillNode> pathToClass = new ArrayList<>();
        SkillNode current = node;
        
        while (current != null) {
            pathToClass.add(current);
            if (current.depth() == 0) {
                break;
            }
            current = current.parent().orElse(null);
        }
        
        Collections.reverse(pathToClass);
        
        double multiplier = 1.0;
        for (int i = 0; i < pathToClass.size(); i++) {
            SkillNode pathNode = pathToClass.get(i);
            long nodeXp = playerData.getIntrinsicXp(pathNode.id());
            long xpToAdd = (long) (amount * multiplier);
            
            if (nodeXp >= pathNode.maxXp()) {
                System.out.println("[XP Flow] " + pathNode.id() + " is maxed, passing through (depth: " + pathNode.depth() + ")");
            } else {
                long newXp = Math.min(nodeXp + xpToAdd, pathNode.maxXp());
                playerData.setIntrinsicXp(pathNode.id(), newXp);
                
                System.out.println("[XP Flow] " + pathNode.id() + " +=" + xpToAdd + " XP (depth: " + pathNode.depth() + 
                    ", multiplier: " + String.format("%.2f", multiplier) + ")");
            }
            
            if (i < pathToClass.size() - 1) {
                multiplier *= config.parentXpDecay();
            }
        }
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
        
        if (aggregator != null) {
            aggregator.updatePlayerWeights(playerData.playerId(), playerData);
        }
    }
    
    public EffectiveXp getEffectiveXp(PlayerSkillData playerData, String skillId) {
        Objects.requireNonNull(playerData, "playerData cannot be null");
        Objects.requireNonNull(skillId, "skillId cannot be null");
        
        List<SkillNode> nodes = treeRegistry.findAllNodes(skillId);
        if (nodes.isEmpty()) {
            return EffectiveXp.zero();
        }
        
        if (nodes.size() == 1) {
            return calculator.calculateEffectiveXp(playerData, nodes.get(0));
        }
        
        return aggregateMultiTreeXp(playerData, nodes);
    }
    
    private EffectiveXp aggregateMultiTreeXp(PlayerSkillData playerData, List<SkillNode> nodes) {
        if (aggregator == null) {
            return calculator.calculateEffectiveXp(playerData, nodes.get(0));
        }
        
        Map<SkillTree, EffectiveXp> treeXpValues = new HashMap<>();
        
        for (SkillNode node : nodes) {
            SkillTree tree = findTreeForNode(node);
            if (tree == null) continue;
            
            EffectiveXp xp = calculator.calculateEffectiveXp(playerData, node);
            treeXpValues.put(tree, xp);
        }
        
        return aggregator.aggregateXp(playerData.playerId(), nodes.get(0).id(), treeXpValues);
    }
    
    private SkillTree findTreeForNode(SkillNode node) {
        for (SkillTree tree : treeRegistry.getAllTrees()) {
            if (tree.getNode(node.id()).isPresent()) {
                return tree;
            }
        }
        return null;
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
