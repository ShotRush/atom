package org.shotrush.atom.tree;

import org.shotrush.atom.model.SkillNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillTreeRegistry {
    
    private final Map<String, SkillTree> trees;
    private final Map<String, Set<String>> skillToTreesIndex;
    
    public SkillTreeRegistry() {
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
                    if (treeNames.isEmpty()) {
                        skillToTreesIndex.remove(skillId);
                    }
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
            if (node.isPresent()) {
                return node;
            }
        }
        
        if (skillId.startsWith("dynamic_")) {
            return generateDynamicNode(skillId);
        }
        
        return Optional.empty();
    }
    
    private Optional<SkillNode> generateDynamicNode(String skillId) {
        String[] parts = skillId.split("\\.");
        if (parts.length < 4) return Optional.empty();
        
        String parentId = String.join(".", java.util.Arrays.copyOf(parts, parts.length - 1));
        Optional<SkillNode> parentOpt = findNode(parentId);
        
        if (parentOpt.isEmpty() || parentOpt.get().depth() < 2) {
            return Optional.empty();
        }
        
        String displayName = parts[parts.length - 1].replace("_", " ");
        displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
        
        SkillNode dynamicNode = org.shotrush.atom.model.SkillNode.builder()
            .id(skillId)
            .displayName(displayName)
            .maxXp(5000)
            .type(org.shotrush.atom.model.SkillNode.NodeType.LEAF)
            .parent(parentOpt.get())
            .build();
        
        return Optional.of(dynamicNode);
    }
    
    public List<SkillNode> findAllNodes(String skillId) {
        List<SkillNode> nodes = new ArrayList<>();
        for (SkillTree tree : trees.values()) {
            tree.getNode(skillId).ifPresent(nodes::add);
        }
        if (skillId.startsWith("dynamic_")) {
            Optional<SkillNode> dynamicNode = generateDynamicNode(skillId);
            dynamicNode.ifPresent(nodes::add);
        }
        return nodes;
    }
    
    public void clear() {
        trees.clear();
        skillToTreesIndex.clear();
    }
    
    public int treeCount() {
        return trees.size();
    }
    
    public boolean isEmpty() {
        return trees.isEmpty();
    }
}
