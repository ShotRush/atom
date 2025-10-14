package org.shotrush.atom.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ActionContext {
    private final String actionId;
    private final TriggerType triggerType;
    private final Material material;
    private final EntityType entityType;
    private final ItemStack tool;
    private final Map<String, Object> metadata;

    public ActionContext(String actionId, TriggerType triggerType, Material material, 
                        EntityType entityType, ItemStack tool) {
        this.actionId = actionId;
        this.triggerType = triggerType;
        this.material = material;
        this.entityType = entityType;
        this.tool = tool;
        this.metadata = new HashMap<>();
    }

    public String getActionId() {
        return actionId;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public Material getMaterial() {
        return material;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public ItemStack getTool() {
        return tool;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public boolean hasBreakingComponent() {
        return triggerType == TriggerType.BLOCK_BREAK || 
               triggerType == TriggerType.FISH_CATCH;
    }

    public boolean hasPlacingComponent() {
        return triggerType == TriggerType.BLOCK_PLACE;
    }

    public boolean hasCombatComponent() {
        return triggerType == TriggerType.ENTITY_DEATH || 
               triggerType == TriggerType.ENTITY_DAMAGE;
    }

    public boolean hasCraftingComponent() {
        return triggerType == TriggerType.CRAFT_ITEM || 
               triggerType == TriggerType.SMITHING;
    }

    public boolean hasProcessingComponent() {
        return triggerType == TriggerType.FURNACE_EXTRACT;
    }

    public boolean hasInteractionComponent() {
        return triggerType == TriggerType.VILLAGER_TRADE || 
               triggerType == TriggerType.ENTITY_BREED ||
               triggerType == TriggerType.ENCHANT_ITEM;
    }

    public boolean usesTool() {
        return tool != null && tool.getType() != Material.AIR;
    }

    public boolean isResourceGathering() {
        return hasBreakingComponent() && material != null;
    }

    public boolean isCreation() {
        return hasCraftingComponent() || hasPlacingComponent();
    }
}
