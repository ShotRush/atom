package org.shotrush.atom.tree;

import org.shotrush.atom.model.Models.EffectiveXp;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;
import org.shotrush.atom.progression.DepthProgression;
import org.shotrush.atom.progression.DepthProgression.SpecializationMetrics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Trees {
    
    private Trees() {}

    public static final class SkillTree {
        private final String name;
        private final double weight;
        private final SkillNode root;
        private final Map<String, SkillNode> nodeIndex;
        
        private SkillTree(String name, double weight, SkillNode root) {
            this.name = name;
            this.weight = weight;
            this.root = root;
            this.nodeIndex = new ConcurrentHashMap<>();
            indexNodes(root);
        }
        
        private void indexNodes(SkillNode node) {
            nodeIndex.put(node.id(), node);
            for (SkillNode child : node.children().values()) {
                indexNodes(child);
            }
        }
        
        public String name() { return name; }
        public double weight() { return weight; }
        public SkillNode root() { return root; }
        
        public Optional<SkillNode> getNode(String skillId) {
            return Optional.ofNullable(nodeIndex.get(skillId));
        }
        
        public boolean containsNode(String skillId) {
            return nodeIndex.containsKey(skillId);
        }
        
        public Set<String> getAllSkillIds() {
            return Collections.unmodifiableSet(nodeIndex.keySet());
        }
        
        public List<SkillNode> getLeafNodes() {
            return nodeIndex.values().stream().filter(SkillNode::isLeaf).toList();
        }
        
        public List<SkillNode> getRootNodes() {
            return nodeIndex.values().stream().filter(SkillNode::isRoot).toList();
        }
        
        public int size() {
            return nodeIndex.size();
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static final class Builder {
            private String name;
            private double weight = 1.0;
            private SkillNode root;
            
            private Builder() {}
            
            public Builder name(String name) {
                this.name = name;
                return this;
            }
            
            public Builder weight(double weight) {
                this.weight = weight;
                return this;
            }
            
            public Builder root(SkillNode root) {
                this.root = root;
                return this;
            }
            
            public SkillTree build() {
                Objects.requireNonNull(name, "name cannot be null");
                Objects.requireNonNull(root, "root cannot be null");
                if (weight <= 0) throw new IllegalArgumentException("weight must be positive");
                return new SkillTree(name, weight, root);
            }
        }
    }

    public static final class Registry {
        private final Map<String, SkillTree> trees;
        private final Map<String, Set<String>> skillToTreesIndex;
        
        public Registry() {
            this.trees = new ConcurrentHashMap<>();
            this.skillToTreesIndex = new ConcurrentHashMap<>();
        }
        
        public void registerTree(SkillTree tree) {
            Objects.requireNonNull(tree, "tree cannot be null");
            trees.put(tree.name(), tree);
            for (String skillId : tree.getAllSkillIds()) {
                skillToTreesIndex.computeIfAbsent(skillId, k -> ConcurrentHashMap.newKeySet())
                    .add(tree.name());
            }
        }
        
        public void unregisterTree(String treeName) {
            SkillTree removed = trees.remove(treeName);
            if (removed != null) {
                for (String skillId : removed.getAllSkillIds()) {
                    Set<String> treeNames = skillToTreesIndex.get(skillId);
                    if (treeNames != null) {
                        treeNames.remove(treeName);
                        if (treeNames.isEmpty()) skillToTreesIndex.remove(skillId);
                    }
                }
            }
        }
        
        public Optional<SkillTree> getTree(String treeName) {
            return Optional.ofNullable(trees.get(treeName));
        }
        
        public Collection<SkillTree> getAllTrees() {
            return Collections.unmodifiableCollection(trees.values());
        }
        
        public Set<String> getTreesContainingSkill(String skillId) {
            Set<String> treeNames = skillToTreesIndex.get(skillId);
            return treeNames != null ? Collections.unmodifiableSet(treeNames) : Collections.emptySet();
        }
        
        public Optional<SkillNode> findNode(String skillId) {
            for (SkillTree tree : trees.values()) {
                Optional<SkillNode> node = tree.getNode(skillId);
                if (node.isPresent()) return node;
            }
            if (skillId.startsWith("dynamic_")) return generateDynamicNode(skillId);
            return Optional.empty();
        }
        
        private Optional<SkillNode> generateDynamicNode(String skillId) {
            String[] parts = skillId.split("\\.");
            if (parts.length < 4) return Optional.empty();
            
            String parentId = String.join(".", Arrays.copyOf(parts, parts.length - 1));
            Optional<SkillNode> parentOpt = findNode(parentId);
            
            if (parentOpt.isEmpty() || parentOpt.get().depth() < 2) return Optional.empty();
            
            String displayName = parts[parts.length - 1].replace("_", " ");
            displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
            
            return Optional.of(SkillNode.builder()
                .id(skillId)
                .displayName(displayName)
                .maxXp(5000)
                .type(SkillNode.NodeType.LEAF)
                .parent(parentOpt.get())
                .build());
        }
        
        public List<SkillNode> findAllNodes(String skillId) {
            List<SkillNode> nodes = new ArrayList<>();
            for (SkillTree tree : trees.values()) {
                tree.getNode(skillId).ifPresent(nodes::add);
            }
            if (skillId.startsWith("dynamic_")) {
                generateDynamicNode(skillId).ifPresent(nodes::add);
            }
            return nodes;
        }
        
        public void clear() {
            trees.clear();
            skillToTreesIndex.clear();
        }
        
        public int treeCount() { return trees.size(); }
        public boolean isEmpty() { return trees.isEmpty(); }
    }

    public static final class Aggregator {
        private final Registry registry;
        private final Map<UUID, Map<String, Double>> playerTreeWeights;
        
        public Aggregator(Registry registry) {
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
        
        public EffectiveXp aggregateXp(UUID playerId, String skillId, Map<SkillTree, EffectiveXp> treeXpValues) {
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
                
                if (maxXp == 0) maxXp = (int) (xp.intrinsicXp() + xp.honoraryXp());
            }
            
            if (totalWeight == 0.0) return EffectiveXp.zero();
            
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
}
