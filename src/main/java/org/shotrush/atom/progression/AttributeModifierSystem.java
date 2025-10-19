package org.shotrush.atom.progression;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;
import org.shotrush.atom.progression.DepthProgression.SpecializationMetrics;

import java.util.*;

import static org.bukkit.attribute.Attribute.*;

import static org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE;


public final class AttributeModifierSystem {
    
    private final Plugin plugin;
    private final Map<String, AttributeMapping> attributeMappings;
    
    public AttributeModifierSystem(Plugin plugin) {
        this.plugin = plugin;
        this.attributeMappings = new HashMap<>();
        initializeAttributeMappings();
    }
    
    
    private void initializeAttributeMappings() {
        
        addMapping("miner", Attribute.BLOCK_BREAK_SPEED, 0.05, 2.0);
        addMapping("miner", Attribute.MINING_EFFICIENCY, 0.03, 1.5);
        addMapping("miner.ore_mining", Attribute.LUCK, 0.5, 5.0); 
        addMapping("miner.stone_mining", Attribute.BLOCK_BREAK_SPEED, 0.08, 2.5);
        
        
        addMapping("guardsman", Attribute.ATTACK_DAMAGE, 0.5, 10.0);
        addMapping("guardsman", Attribute.ARMOR, 1.0, 15.0);
        addMapping("guardsman.combat", Attribute.ATTACK_DAMAGE, 1.0, 15.0);
        addMapping("guardsman.combat", Attribute.ATTACK_SPEED, 0.05, 0.5);
        addMapping("guardsman.defense", Attribute.ARMOR, 2.0, 20.0);
        addMapping("guardsman.defense", Attribute.ARMOR_TOUGHNESS, 1.0, 10.0);
        addMapping("guardsman.defense", KNOCKBACK_RESISTANCE, 0.05, 0.5);
        
        
        addMapping("farmer", Attribute.LUCK, 0.3, 3.0); 
        addMapping("farmer", Attribute.MOVEMENT_SPEED, 0.01, 0.1);
        addMapping("farmer.crop_farming", Attribute.LUCK, 0.5, 5.0);
        addMapping("farmer.animal_husbandry", Attribute.LUCK, 0.4, 4.0);
        
        
        addMapping("builder", Attribute.BLOCK_INTERACTION_RANGE, 0.1, 2.0);
        addMapping("builder", Attribute.ENTITY_INTERACTION_RANGE, 0.1, 2.0);
        addMapping("builder.advanced_building", Attribute.MOVEMENT_SPEED, 0.01, 0.15);
        
        
        addMapping("blacksmith", Attribute.ATTACK_DAMAGE, 0.3, 5.0);
        addMapping("blacksmith", Attribute.ARMOR, 0.5, 8.0);
        addMapping("blacksmith.tool_crafting", Attribute.MINING_EFFICIENCY, 0.05, 1.0);
        addMapping("blacksmith.armor_crafting", Attribute.ARMOR_TOUGHNESS, 0.5, 5.0);
        
        
        addMapping("healer", Attribute.MAX_HEALTH, 1.0, 10.0);
        addMapping("healer.brewing", Attribute.LUCK, 0.2, 2.0);
        addMapping("healer.support", Attribute.MAX_HEALTH, 2.0, 20.0);
        
        
        addMapping("librarian", Attribute.LUCK, 0.4, 4.0);
        addMapping("librarian.enchanting", Attribute.LUCK, 0.6, 6.0);
    }
    
    
    private void addMapping(String skillPath, Attribute attribute, double perLevel, double maxBonus) {
        attributeMappings.put(skillPath + ":" + attribute.getKey().getKey(), 
            new AttributeMapping(skillPath, attribute, perLevel, maxBonus));
    }
    
    
    public void updatePlayerAttributes(
            Player player,
            PlayerSkillData playerData,
            Map<String, SkillNode> allNodes,
            Map<String, SpecializationMetrics> specializationMetrics) {
        
        
        clearAtomModifiers(player);
        
        
        Map<Attribute, Double> attributeChanges = new HashMap<>();
        
        for (Map.Entry<String, Long> entry : playerData.getAllIntrinsicXp().entrySet()) {
            String skillId = entry.getKey();
            SkillNode node = allNodes.get(skillId);
            if (node == null) continue;
            
            String treeName = extractTreeName(skillId);
            SpecializationMetrics metrics = specializationMetrics.getOrDefault(treeName, 
                new SpecializationMetrics(0, 0, 0.0, 0.0));
            
            
            Map<Attribute, Double> skillModifiers = calculateSkillModifiers(
                node, playerData, metrics);
            
            
            for (Map.Entry<Attribute, Double> mod : skillModifiers.entrySet()) {
                attributeChanges.merge(mod.getKey(), mod.getValue(), Double::sum);
            }
        }
        
        
        for (Map.Entry<Attribute, Double> entry : attributeChanges.entrySet()) {
            applyAttributeModifier(player, entry.getKey(), entry.getValue());
        }
    }
    
    
    private Map<Attribute, Double> calculateSkillModifiers(
            SkillNode node,
            PlayerSkillData playerData,
            SpecializationMetrics metrics) {
        
        Map<Attribute, Double> modifiers = new HashMap<>();
        
        
        long currentXp = playerData.getAllIntrinsicXp().getOrDefault(node.id(), 0L);
        double xpRatio = (double) currentXp / node.maxXp();
        
        
        if (xpRatio < 0.1) {
            return modifiers;
        }
        
        
        double penaltyMultiplier = DepthProgression.calculatePenaltyMultiplier(node, playerData, metrics);
        double bonusMultiplier = DepthProgression.calculateBonusMultiplier(node, playerData, metrics);
        
        
        for (AttributeMapping mapping : attributeMappings.values()) {
            if (node.id().startsWith(mapping.skillPath)) {
                double baseValue = mapping.perLevel * xpRatio;
                double finalValue = baseValue * bonusMultiplier * penaltyMultiplier;
                
                
                finalValue = Math.min(finalValue, mapping.maxBonus);
                
                modifiers.merge(mapping.attribute, finalValue, Double::sum);
            }
        }
        
        return modifiers;
    }
    
    
    private void applyAttributeModifier(Player player, Attribute attribute, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        
        
        NamespacedKey key = new NamespacedKey(plugin, "atom_" + attribute.getKey().getKey());
        
        
        AttributeModifier.Operation operation = getOperationForAttribute(attribute);
        
        
        AttributeModifier modifier = new AttributeModifier(
            key,
            amount,
            operation,
            EquipmentSlotGroup.ANY
        );
        
        instance.addModifier(modifier);
    }
    
    
    private AttributeModifier.Operation getOperationForAttribute(Attribute attribute) {

        if (attribute == BLOCK_BREAK_SPEED || attribute == MINING_EFFICIENCY || 
            attribute == MOVEMENT_SPEED || attribute == ATTACK_SPEED ||
            attribute == KNOCKBACK_RESISTANCE) {
            return AttributeModifier.Operation.ADD_SCALAR;
        }
        return AttributeModifier.Operation.ADD_NUMBER;
    }
    
    
    public void clearAtomModifiers(Player player) {
        for (Attribute attribute : Registry.ATTRIBUTE) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            
            instance.getModifiers().stream()
                .filter(mod -> mod.getKey().getNamespace().equals(plugin.getName().toLowerCase()))
                .forEach(instance::removeModifier);
        }
    }
    
    private String extractTreeName(String skillId) {
        int firstDot = skillId.indexOf('.');
        return firstDot > 0 ? skillId.substring(0, firstDot) : skillId;
    }
    
    
    private record AttributeMapping(
        String skillPath,
        Attribute attribute,
        double perLevel,
        double maxBonus
    ) {}
}
