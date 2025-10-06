package org.shotrush.atom.skill;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSkillData {
    private final UUID playerId;
    private final Map<SkillType, SkillData> skills = new EnumMap<>(SkillType.class);
    
    public PlayerSkillData(UUID playerId) {
        this.playerId = playerId;
        for (SkillType type : SkillType.values()) {
            skills.put(type, new SkillData());
        }
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public SkillData getSkillData(SkillType type) {
        return skills.get(type);
    }
    
    public Map<SkillType, SkillData> getAllSkills() {
        return skills;
    }
}
