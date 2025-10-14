package org.shotrush.atom.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Set;

public class TrackedAction {
    private final String id;
    private final String displayName;
    private final String description;
    private final List<ActionTrigger> triggers;
    private final double experience;

    public TrackedAction(String id, String displayName, String description, 
                        List<ActionTrigger> triggers, double experience) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.triggers = triggers;
        this.experience = experience;
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

    public List<ActionTrigger> getTriggers() {
        return triggers;
    }

    public double getExperience() {
        return experience;
    }

    public static class ActionTrigger {
        private final TriggerType type;
        private final Set<Material> materials;
        private final Set<EntityType> entities;
        private final boolean matchAny;

        public ActionTrigger(TriggerType type, Set<Material> materials, 
                           Set<EntityType> entities, boolean matchAny) {
            this.type = type;
            this.materials = materials;
            this.entities = entities;
            this.matchAny = matchAny;
        }

        public TriggerType getType() {
            return type;
        }

        public Set<Material> getMaterials() {
            return materials;
        }

        public Set<EntityType> getEntities() {
            return entities;
        }

        public boolean isMatchAny() {
            return matchAny;
        }

        public boolean matches(Material material) {
            return matchAny || (materials != null && materials.contains(material));
        }

        public boolean matches(EntityType entity) {
            return matchAny || (entities != null && entities.contains(entity));
        }
    }
}
