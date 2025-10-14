package org.shotrush.atom.model;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Restriction {
    private final String id;
    private final String displayName;
    private final String description;
    private final List<RestrictionBlock> blocks;
    private final Map<String, Double> actionRequirements;
    private final String denyMessage;
    private final boolean allowFirstTime;
    private final int firstTimeMaxLevel;

    public Restriction(String id, String displayName, String description,
                      List<RestrictionBlock> blocks, Map<String, Double> actionRequirements,
                      String denyMessage, boolean allowFirstTime, int firstTimeMaxLevel) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.blocks = blocks;
        this.actionRequirements = actionRequirements;
        this.denyMessage = denyMessage;
        this.allowFirstTime = allowFirstTime;
        this.firstTimeMaxLevel = firstTimeMaxLevel;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public List<RestrictionBlock> getBlocks() {
        return blocks;
    }

    public Map<String, Double> getActionRequirements() {
        return actionRequirements;
    }

    public String getDenyMessage() {
        return denyMessage;
    }

    public boolean isAllowFirstTime() {
        return allowFirstTime;
    }

    public int getFirstTimeMaxLevel() {
        return firstTimeMaxLevel;
    }

    public static class RestrictionBlock {
        private final TriggerType type;
        private final Set<Material> materials;
        private final Set<Material> items;

        public RestrictionBlock(TriggerType type, Set<Material> materials, Set<Material> items) {
            this.type = type;
            this.materials = materials;
            this.items = items;
        }

        public TriggerType getType() {
            return type;
        }

        public Set<Material> getMaterials() {
            return materials;
        }

        public Set<Material> getItems() {
            return items;
        }

        public boolean matches(Material material) {
            return materials != null && materials.contains(material);
        }

        public boolean matchesItem(Material item) {
            return items != null && items.contains(item);
        }
    }
}
