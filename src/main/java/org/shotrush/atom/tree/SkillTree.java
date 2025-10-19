package org.shotrush.atom.tree;

import org.shotrush.atom.model.SkillNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillTree {
    
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
    
    public String name() {
        return name;
    }
    
    public double weight() {
        return weight;
    }
    
    public SkillNode root() {
        return root;
    }
    
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
        return nodeIndex.values().stream()
            .filter(SkillNode::isLeaf)
            .toList();
    }
    
    public List<SkillNode> getRootNodes() {
        return nodeIndex.values().stream()
            .filter(SkillNode::isRoot)
            .toList();
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
            if (weight <= 0) {
                throw new IllegalArgumentException("weight must be positive");
            }
            return new SkillTree(name, weight, root);
        }
    }
}
