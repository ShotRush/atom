package org.shotrush.atom.manager;

import org.shotrush.atom.skill.PlayerSkillData;
import org.shotrush.atom.skill.SkillData;
import org.shotrush.atom.skill.SkillType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillManager {
    private final Map<UUID, PlayerSkillData> playerData = new HashMap<>();
    
    public PlayerSkillData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlayerSkillData::new);
    }
    
    public SkillData getSkillData(UUID playerId, SkillType type) {
        return getPlayerData(playerId).getSkillData(type);
    }
    
    public void addExperience(UUID playerId, SkillType type, String itemKey, int amount, int maxExperience) {
        getSkillData(playerId, type).addExperience(itemKey, amount, maxExperience);
    }
    
    public int getExperience(UUID playerId, SkillType type, String itemKey) {
        return getSkillData(playerId, type).getExperience(itemKey);
    }
    
    public double getSuccessRate(UUID playerId, SkillType type, String itemKey, double baseRate, double maxRate, int expPerLevel) {
        return getSkillData(playerId, type).getSuccessRate(itemKey, baseRate, maxRate, expPerLevel);
    }
    
    public void removePlayer(UUID playerId) {
        playerData.remove(playerId);
    }
    
    public Map<UUID, PlayerSkillData> getAllPlayerData() {
        return playerData;
    }
}
