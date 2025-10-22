package org.shotrush.atom.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillNode {
    
    private final String id;
    private final String displayName;
    private final SkillNode parent;
    private final Map<String, SkillNode> children;
    private final int maxXp;
    private final NodeType type;
    
    private SkillNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id cannot be null");
        this.displayName = Objects.requireNonNull(builder.displayName, "displayName cannot be null");
        this.parent = builder.parent;
        this.children = new ConcurrentHashMap<>(builder.children);
        this.maxXp = builder.maxXp;
        this.type = Objects.requireNonNull(builder.type, "type cannot be null");
    }
    
    public String id() {
        return id;
    }
    
    public String displayName() {
        return displayName;
    }
    
    public Optional<SkillNode> parent() {
        return Optional.ofNullable(parent);
    }
    
    public Map<String, SkillNode> children() {
        return Collections.unmodifiableMap(children);
    }
    
    public int maxXp() {
        return maxXp;
    }
    
    public NodeType type() {
        return type;
    }
    
    public boolean isRoot() {
        return parent == null;
    }
    
    public boolean isLeaf() {
        return children.isEmpty();
    }
    
    public List<SkillNode> ancestors() {
        List<SkillNode> ancestors = new ArrayList<>();
        SkillNode current = parent;
        while (current != null) {
            ancestors.add(current);
            current = current.parent;
        }
        return ancestors;
    }
    
    public List<SkillNode> descendants() {
        List<SkillNode> descendants = new ArrayList<>();
        collectDescendants(this, descendants);
        return descendants;
    }
    
    private void collectDescendants(SkillNode node, List<SkillNode> accumulator) {
        for (SkillNode child : node.children.values()) {
            accumulator.add(child);
            collectDescendants(child, accumulator);
        }
    }
    
    public int depth() {
        int depth = 0;
        SkillNode current = parent;
        while (current != null) {
            depth++;
            current = current.parent;
        }
        return depth;
    }
    
    public void addChild(SkillNode child) {
        children.put(child.id(), child);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SkillNode other)) return false;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "SkillNode{id='" + id + "', displayName='" + displayName + "', type=" + type + "}";
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private String id;
        private String displayName;
        private SkillNode parent;
        private Map<String, SkillNode> children = new HashMap<>();
        private int maxXp = 10000;
        private NodeType type = NodeType.LEAF;
        
        private Builder() {}
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public Builder parent(SkillNode parent) {
            this.parent = parent;
            return this;
        }
        
        public Builder children(Map<String, SkillNode> children) {
            this.children = new HashMap<>(children);
            return this;
        }
        
        public Builder addChild(SkillNode child) {
            this.children.put(child.id(), child);
            return this;
        }
        
        public Builder maxXp(int maxXp) {
            this.maxXp = maxXp;
            return this;
        }
        
        public Builder type(NodeType type) {
            this.type = type;
            return this;
        }
        
        public SkillNode build() {
            return new SkillNode(this);
        }
    }
    
    public enum NodeType {
        ROOT,
        BRANCH,
        LEAF
    }
}
