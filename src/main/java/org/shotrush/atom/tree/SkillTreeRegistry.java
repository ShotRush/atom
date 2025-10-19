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
        return Optional.empty();
    }
    
    public List<SkillNode> findAllNodes(String skillId) {
        List<SkillNode> nodes = new ArrayList<>();
        for (SkillTree tree : trees.values()) {
            tree.getNode(skillId).ifPresent(nodes::add);
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
