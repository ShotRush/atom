package org.shotrush.atom.milestone;

import org.shotrush.atom.effects.EffectManager;
import org.shotrush.atom.engine.XpEngine;
import org.shotrush.atom.model.PlayerSkillData;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MilestoneManager {
    
    private final XpEngine xpEngine;
    private final EffectManager effectManager;
    private final List<Milestone> milestones;
    private final Map<UUID, Set<String>> completedMilestones;
    
    public MilestoneManager(XpEngine xpEngine, EffectManager effectManager) {
        this.xpEngine = xpEngine;
        this.effectManager = effectManager;
        this.milestones = createDefaultMilestones();
        this.completedMilestones = new ConcurrentHashMap<>();
    }
    
    private List<Milestone> createDefaultMilestones() {
        return List.of(
            new Milestone("farmer_novice", "Novice Farmer", "Reach 25% farming skill", 
                "farmer", 25.0, Milestone.MilestoneReward.REDUCED_PENALTY),
            new Milestone("farmer_apprentice", "Apprentice Farmer", "Reach 50% farming skill", 
                "farmer", 50.0, Milestone.MilestoneReward.BONUS_DROP_RATE),
            new Milestone("farmer_expert", "Expert Farmer", "Reach 75% farming skill", 
                "farmer", 75.0, Milestone.MilestoneReward.BONUS_XP_GAIN),
            
            new Milestone("miner_novice", "Novice Miner", "Reach 25% mining skill", 
                "miner", 25.0, Milestone.MilestoneReward.REDUCED_PENALTY),
            new Milestone("miner_apprentice", "Apprentice Miner", "Reach 50% mining skill", 
                "miner", 50.0, Milestone.MilestoneReward.BONUS_DROP_RATE),
            new Milestone("miner_expert", "Expert Miner", "Reach 75% mining skill", 
                "miner", 75.0, Milestone.MilestoneReward.BONUS_XP_GAIN),
            
            new Milestone("guardsman_novice", "Novice Guardsman", "Reach 25% combat skill", 
                "guardsman", 25.0, Milestone.MilestoneReward.REDUCED_PENALTY),
            new Milestone("guardsman_apprentice", "Apprentice Guardsman", "Reach 50% combat skill", 
                "guardsman", 50.0, Milestone.MilestoneReward.BONUS_XP_GAIN),
            new Milestone("guardsman_expert", "Expert Guardsman", "Reach 75% combat skill", 
                "guardsman", 75.0, Milestone.MilestoneReward.BONUS_XP_GAIN),
            
            new Milestone("blacksmith_novice", "Novice Blacksmith", "Reach 25% blacksmith skill", 
                "blacksmith", 25.0, Milestone.MilestoneReward.REDUCED_PENALTY),
            new Milestone("blacksmith_apprentice", "Apprentice Blacksmith", "Reach 50% blacksmith skill", 
                "blacksmith", 50.0, Milestone.MilestoneReward.UNLOCK_FEATURE),
            
            new Milestone("builder_novice", "Novice Builder", "Reach 25% building skill", 
                "builder", 25.0, Milestone.MilestoneReward.REDUCED_PENALTY),
            new Milestone("builder_apprentice", "Apprentice Builder", "Reach 50% building skill", 
                "builder", 50.0, Milestone.MilestoneReward.BONUS_XP_GAIN)
        );
    }
    
    public void checkMilestones(Player player, PlayerSkillData data) {
        Set<String> playerMilestones = completedMilestones.computeIfAbsent(
            player.getUniqueId(), 
            k -> ConcurrentHashMap.newKeySet()
        );
        
        for (Milestone milestone : milestones) {
            if (playerMilestones.contains(milestone.id())) {
                continue;
            }
            
            double currentLevel = xpEngine.getSkillLevel(data, milestone.skillId());
            
            if (currentLevel >= milestone.requiredLevel()) {
                playerMilestones.add(milestone.id());
                effectManager.sendMilestoneReached(player, milestone.displayName());
            }
        }
    }
    
    public List<Milestone> getCompletedMilestones(UUID playerId) {
        Set<String> completed = completedMilestones.get(playerId);
        if (completed == null) return List.of();
        
        return milestones.stream()
            .filter(m -> completed.contains(m.id()))
            .toList();
    }
    
    public List<Milestone> getAvailableMilestones(UUID playerId, PlayerSkillData data) {
        Set<String> completed = completedMilestones.getOrDefault(playerId, Set.of());
        
        return milestones.stream()
            .filter(m -> !completed.contains(m.id()))
            .filter(m -> {
                double currentLevel = xpEngine.getSkillLevel(data, m.skillId());
                return currentLevel < m.requiredLevel();
            })
            .toList();
    }
    
    public boolean hasMilestone(UUID playerId, String milestoneId) {
        Set<String> completed = completedMilestones.get(playerId);
        return completed != null && completed.contains(milestoneId);
    }
    
    public void clearPlayerMilestones(UUID playerId) {
        completedMilestones.remove(playerId);
    }
    
    public List<Milestone> getAllMilestones() {
        return milestones;
    }
}
