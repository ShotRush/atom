package org.shotrush.atom.config;

import java.util.List;
import java.util.Map;

public record TreeDefinition(
    String name,
    double weight,
    NodeDefinition root
) {
    
    public record NodeDefinition(
        String id,
        String displayName,
        int maxXp,
        String type,
        List<NodeDefinition> children
    ) {}
}
