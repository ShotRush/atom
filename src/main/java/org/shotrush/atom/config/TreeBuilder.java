package org.shotrush.atom.config;

import org.shotrush.atom.model.SkillNode;
import org.shotrush.atom.tree.SkillTree;

import java.util.HashMap;
import java.util.Map;

public final class TreeBuilder {
    
    public static SkillTree buildFromDefinition(TreeDefinition definition) {
        SkillNode root = buildNode(definition.root(), null);
        
        return SkillTree.builder()
            .name(definition.name())
            .weight(definition.weight())
            .root(root)
            .build();
    }
    
    private static SkillNode buildNode(TreeDefinition.NodeDefinition def, SkillNode parent) {
        SkillNode.NodeType type = parseNodeType(def.type());
        
        Map<String, SkillNode> children = new HashMap<>();
        
        SkillNode.Builder builder = SkillNode.builder()
            .id(def.id())
            .displayName(def.displayName())
            .maxXp(def.maxXp())
            .type(type)
            .parent(parent);
        
        SkillNode node = builder.build();
        
        if (def.children() != null) {
            for (TreeDefinition.NodeDefinition childDef : def.children()) {
                SkillNode child = buildNode(childDef, node);
                children.put(child.id(), child);
            }
        }
        
        return SkillNode.builder()
            .id(def.id())
            .displayName(def.displayName())
            .maxXp(def.maxXp())
            .type(type)
            .parent(parent)
            .children(children)
            .build();
    }
    
    private static SkillNode.NodeType parseNodeType(String type) {
        if (type == null) {
            return SkillNode.NodeType.LEAF;
        }
        
        return switch (type.toUpperCase()) {
            case "ROOT" -> SkillNode.NodeType.ROOT;
            case "BRANCH" -> SkillNode.NodeType.BRANCH;
            case "LEAF" -> SkillNode.NodeType.LEAF;
            default -> SkillNode.NodeType.LEAF;
        };
    }
}
